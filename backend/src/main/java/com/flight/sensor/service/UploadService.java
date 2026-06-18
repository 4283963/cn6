package com.flight.sensor.service;

import com.flight.sensor.dto.UploadTaskDTO;
import com.flight.sensor.entity.Flight;
import com.flight.sensor.entity.SensorData;
import com.flight.sensor.entity.UploadTask;
import com.flight.sensor.repository.FlightRepository;
import com.flight.sensor.repository.SensorDataRepository;
import com.flight.sensor.repository.UploadTaskRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final int BATCH_SIZE = 1000;
    private static final Map<String, UploadTaskDTO> progressCache = new ConcurrentHashMap<>();
    private static final Set<String> INVALID_NUMERIC_MARKERS = Set.of(
            "nan", "infinity", "-infinity", "+infinity",
            "na", "n/a", "null", "none", "-", "--", "nil", "err", "error", "undefined"
    );

    private final UploadTaskRepository uploadTaskRepository;
    private final FlightRepository flightRepository;
    private final SensorDataRepository sensorDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    public UploadTaskDTO createUploadTask(MultipartFile file, String flightNumber) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        UploadTask task = UploadTask.builder()
                .taskId(taskId)
                .flightNumber(flightNumber)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .status("PENDING")
                .processedRecords(0L)
                .totalRecords(0L)
                .startTime(LocalDateTime.now())
                .build();

        uploadTaskRepository.save(task);

        UploadTaskDTO dto = toDTO(task);
        dto.setProgress(0);
        progressCache.put(taskId, dto);

        processFileAsync(taskId, file, flightNumber);

        return dto;
    }

    @Async
    public void processFileAsync(String taskId, MultipartFile file, String flightNumber) {
        log.info("开始异步处理文件: taskId={}, flightNumber={}", taskId, flightNumber);

        updateTaskStatus(taskId, "PROCESSING", null, null, null);

        Flight flight = null;
        long totalProcessed = 0;
        long skippedLines = 0;
        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = Long.MIN_VALUE;
        LocalDateTime firstTime = null;
        LocalDateTime lastTime = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVReader csvReader = new CSVReader(reader);
            String[] headers = csvReader.readNext();

            if (headers == null || headers.length == 0) {
                throw new RuntimeException("CSV文件为空或格式不正确");
            }

            Map<String, Integer> headerMap = mapHeaders(headers);
            log.debug("CSV表头映射: {}", headerMap);

            Optional<Flight> existingFlight = flightRepository.findByFlightNumber(flightNumber);
            if (existingFlight.isPresent()) {
                flight = existingFlight.get();
                log.info("航班已存在，删除旧数据: flightNumber={}", flightNumber);
                sensorDataRepository.deleteByFlightId(flight.getId());
                flight.setTotalRecords(0L);
            } else {
                flight = Flight.builder()
                        .flightNumber(flightNumber)
                        .totalRecords(0L)
                        .createdAt(LocalDateTime.now())
                        .build();
                flight = flightRepository.save(flight);
            }

            List<SensorData> batch = new ArrayList<>(BATCH_SIZE);
            long lineCount = 0;

            while (true) {
                String[] line;
                try {
                    line = csvReader.readNext();
                } catch (CsvValidationException e) {
                    lineCount++;
                    skippedLines++;
                    log.warn("第{}行CSV格式异常，跳过: {}", lineCount, e.getMessage());
                    continue;
                }
                if (line == null) {
                    break;
                }
                lineCount++;

                try {
                    SensorData sensorData = parseLine(line, headerMap, flight.getId());
                    if (sensorData != null) {
                        batch.add(sensorData);
                        totalProcessed++;

                        if (sensorData.getTimestamp() != null) {
                            long ts = sensorData.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                            if (ts < firstTimestamp) {
                                firstTimestamp = ts;
                                firstTime = sensorData.getTimestamp();
                            }
                            if (ts > lastTimestamp) {
                                lastTimestamp = ts;
                                lastTime = sensorData.getTimestamp();
                            }
                        }

                        if (batch.size() >= BATCH_SIZE) {
                            try {
                                saveBatch(batch);
                            } catch (Exception e) {
                                log.warn("第{}行附近批量写入失败，尝试逐条写入恢复: {}", lineCount, e.getMessage());
                                recoverBatch(batch);
                            }
                            batch.clear();
                            updateTaskProgress(taskId, totalProcessed);
                        }
                    } else {
                        skippedLines++;
                    }
                } catch (Exception e) {
                    skippedLines++;
                    log.warn("解析第{}行数据失败，跳过: {}", lineCount, e.getMessage());
                }

                if (lineCount % 10000 == 0) {
                    log.debug("已处理 {} 行数据，成功 {} 条，跳过 {} 行", lineCount, totalProcessed, skippedLines);
                }
            }

            if (!batch.isEmpty()) {
                try {
                    saveBatch(batch);
                } catch (Exception e) {
                    log.warn("末尾批量写入失败，尝试逐条写入恢复: {}", e.getMessage());
                    recoverBatch(batch);
                }
            }

            final long finalTotalProcessed = totalProcessed;
            final LocalDateTime finalFirstTime = firstTime;
            final LocalDateTime finalLastTime = lastTime;

            flight = flightRepository.findById(flight.getId()).orElse(flight);
            flight.setDepartureTime(finalFirstTime);
            flight.setArrivalTime(finalLastTime);
            flight.setTotalRecords(finalTotalProcessed);
            flightRepository.save(flight);

            if (skippedLines > 0) {
                log.info("文件处理完成: taskId={}, 成功记录数={}, 跳过行数={}", taskId, totalProcessed, skippedLines);
            } else {
                log.info("文件处理完成: taskId={}, 总记录数={}", taskId, totalProcessed);
            }
            updateTaskStatus(taskId, "COMPLETED", totalProcessed, totalProcessed, null);

        } catch (Exception e) {
            log.error("文件处理失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "FAILED", totalProcessed, totalProcessed, e.getMessage());
        }
    }

    private Map<String, Integer> mapHeaders(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase()
                    .replaceAll("[_\\s-]", "");
            map.put(header, i);
        }
        return map;
    }

    private SensorData parseLine(String[] line, Map<String, Integer> headerMap, Long flightId) {
        LocalDateTime timestamp = parseTimestamp(getValue(line, headerMap, "timestamp", "time", "datetime", "date"));
        if (timestamp == null) {
            return null;
        }

        return SensorData.builder()
                .flightId(flightId)
                .timestamp(timestamp)
                .altitude(parseDouble(getValue(line, headerMap, "altitude", "高度", "height", "alt")))
                .speed(parseDouble(getValue(line, headerMap, "speed", "速度", "velocity", "tas", "ias")))
                .engineTemperature(parseDouble(getValue(line, headerMap, "enginetemperature", "egt", "发动机温度", "temp", "temperature", "engtemp")))
                .fuelConsumption(parseDouble(getValue(line, headerMap, "fuelconsumption", "fuel", "燃油消耗", "fuelflow", "ff")))
                .cabinPressure(parseDouble(getValue(line, headerMap, "cabinpressure", "pressure", "客舱压力", "cabpress")))
                .latitude(parseDouble(getValue(line, headerMap, "latitude", "lat", "纬度")))
                .longitude(parseDouble(getValue(line, headerMap, "longitude", "lon", "lng", "经度")))
                .verticalSpeed(parseDouble(getValue(line, headerMap, "verticalspeed", "vs", "垂直速度", "vy")))
                .heading(parseDouble(getValue(line, headerMap, "heading", "hdg", "航向")))
                .build();
    }

    private String getValue(String[] line, Map<String, Integer> headerMap, String... keys) {
        for (String key : keys) {
            String normalizedKey = key.toLowerCase().replaceAll("[_\\s-]", "");
            Integer idx = headerMap.get(normalizedKey);
            if (idx != null && idx < line.length) {
                String value = line[idx];
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            long epochMillis = Long.parseLong(value);
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(epochMillis),
                    java.time.ZoneId.systemDefault()
            );
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        if (INVALID_NUMERIC_MARKERS.contains(trimmed)) {
            return null;
        }
        try {
            double result = Double.parseDouble(trimmed);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return null;
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    protected void saveBatch(List<SensorData> batch) {
        for (SensorData data : batch) {
            entityManager.persist(data);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private void recoverBatch(List<SensorData> batch) {
        try {
            entityManager.clear();
        } catch (Exception ignored) {
        }
        long recovered = 0;
        for (SensorData data : batch) {
            try {
                entityManager.persist(data);
                entityManager.flush();
                entityManager.clear();
                recovered++;
            } catch (Exception e) {
                try {
                    entityManager.clear();
                } catch (Exception ignored) {
                }
                log.debug("单条写入失败，跳过: {}", e.getMessage());
            }
        }
        if (recovered > 0) {
            log.info("批量恢复写入完成，成功恢复 {} 条记录", recovered);
        }
    }

    private void updateTaskStatus(String taskId, String status, Long processed, Long total, String errorMsg) {
        uploadTaskRepository.findByTaskId(taskId).ifPresent(task -> {
            task.setStatus(status);
            if (processed != null) task.setProcessedRecords(processed);
            if (total != null) task.setTotalRecords(total);
            if (errorMsg != null) task.setErrorMessage(errorMsg);
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setEndTime(LocalDateTime.now());
            }
            uploadTaskRepository.save(task);

            UploadTaskDTO dto = toDTO(task);
            if (total != null && total > 0) {
                dto.setProgress((int) ((processed * 100) / total));
            } else if ("COMPLETED".equals(status)) {
                dto.setProgress(100);
            }
            progressCache.put(taskId, dto);
        });
    }

    private void updateTaskProgress(String taskId, long processed) {
        uploadTaskRepository.findByTaskId(taskId).ifPresent(task -> {
            task.setProcessedRecords(processed);
            if (task.getTotalRecords() == null || task.getTotalRecords() == 0) {
                task.setTotalRecords(processed);
            }
            uploadTaskRepository.save(task);

            UploadTaskDTO dto = toDTO(task);
            dto.setProgress(calculateProgress(task, processed));
            progressCache.put(taskId, dto);
        });
    }

    private int calculateProgress(UploadTask task, long processed) {
        if (task.getFileSize() != null && task.getFileSize() > 0) {
            long estimatedTotal = (processed * 300);
            if (estimatedTotal > 0) {
                int progress = (int) Math.min(99, (processed * 100) / (task.getFileSize() / 150 + 1));
                return progress;
            }
        }
        if (processed > 10000) return 50;
        if (processed > 5000) return 30;
        if (processed > 1000) return 15;
        if (processed > 100) return 5;
        return 1;
    }

    public UploadTaskDTO getTaskProgress(String taskId) {
        UploadTaskDTO cached = progressCache.get(taskId);
        if (cached != null) {
            return cached;
        }

        return uploadTaskRepository.findByTaskId(taskId)
                .map(task -> {
                    UploadTaskDTO dto = toDTO(task);
                    if ("COMPLETED".equals(task.getStatus())) {
                        dto.setProgress(100);
                    } else if (task.getProcessedRecords() != null && task.getTotalRecords() != null
                            && task.getTotalRecords() > 0) {
                        dto.setProgress((int) ((task.getProcessedRecords() * 100) / task.getTotalRecords()));
                    } else {
                        dto.setProgress(0);
                    }
                    return dto;
                })
                .orElse(null);
    }

    public List<UploadTaskDTO> getAllTasks() {
        return uploadTaskRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(task -> {
                    UploadTaskDTO dto = toDTO(task);
                    if ("COMPLETED".equals(task.getStatus())) {
                        dto.setProgress(100);
                    } else if (task.getProcessedRecords() != null && task.getTotalRecords() != null
                            && task.getTotalRecords() > 0) {
                        dto.setProgress((int) ((task.getProcessedRecords() * 100) / task.getTotalRecords()));
                    } else {
                        dto.setProgress(0);
                    }
                    return dto;
                })
                .toList();
    }

    private UploadTaskDTO toDTO(UploadTask task) {
        return UploadTaskDTO.builder()
                .taskId(task.getTaskId())
                .flightNumber(task.getFlightNumber())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .status(task.getStatus())
                .processedRecords(task.getProcessedRecords())
                .totalRecords(task.getTotalRecords())
                .errorMessage(task.getErrorMessage())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .build();
    }
}
