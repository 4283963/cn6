package com.flight.sensor.service;

import com.flight.sensor.dto.FlightDataResponse;
import com.flight.sensor.dto.FlightDTO;
import com.flight.sensor.dto.SensorDataDTO;
import com.flight.sensor.dto.SensorSummaryDTO;
import com.flight.sensor.entity.Flight;
import com.flight.sensor.entity.SensorData;
import com.flight.sensor.repository.FlightRepository;
import com.flight.sensor.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final FlightRepository flightRepository;
    private final SensorDataRepository sensorDataRepository;
    private final FlightService flightService;

    public FlightDataResponse getFlightDataByFlightNumber(String flightNumber) {
        Optional<Flight> flightOpt = flightRepository.findByFlightNumber(flightNumber);
        if (flightOpt.isEmpty()) {
            return null;
        }

        Flight flight = flightOpt.get();
        List<SensorData> sensorDataList = sensorDataRepository.findByFlightIdOrderByTimestampAsc(flight.getId());

        List<SensorDataDTO> sensorDataDTOs = sensorDataList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        SensorSummaryDTO summary = calculateSummary(sensorDataList);

        return FlightDataResponse.builder()
                .flight(flightService.getFlightById(flight.getId()).orElse(null))
                .sensorData(sensorDataDTOs)
                .summary(summary)
                .build();
    }

    public FlightDataResponse getFlightDataByFlightNumberWithSampling(String flightNumber, int maxPoints) {
        Optional<Flight> flightOpt = flightRepository.findByFlightNumber(flightNumber);
        if (flightOpt.isEmpty()) {
            return null;
        }

        Flight flight = flightOpt.get();
        List<SensorData> sensorDataList = sensorDataRepository.findByFlightIdOrderByTimestampAsc(flight.getId());

        List<SensorData> sampledData = sampleData(sensorDataList, maxPoints);

        List<SensorDataDTO> sensorDataDTOs = sampledData.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        SensorSummaryDTO summary = calculateSummary(sensorDataList);

        return FlightDataResponse.builder()
                .flight(flightService.getFlightById(flight.getId()).orElse(null))
                .sensorData(sensorDataDTOs)
                .summary(summary)
                .build();
    }

    public FlightDataResponse getFlightDataByFlightNumberWithTimeRange(
            String flightNumber, LocalDateTime start, LocalDateTime end) {
        Optional<Flight> flightOpt = flightRepository.findByFlightNumber(flightNumber);
        if (flightOpt.isEmpty()) {
            return null;
        }

        Flight flight = flightOpt.get();
        List<SensorData> sensorDataList;
        if (start != null && end != null) {
            sensorDataList = sensorDataRepository.findByFlightIdAndTimestampBetween(flight.getId(), start, end);
        } else {
            sensorDataList = sensorDataRepository.findByFlightIdOrderByTimestampAsc(flight.getId());
        }

        List<SensorDataDTO> sensorDataDTOs = sensorDataList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        SensorSummaryDTO summary = calculateSummary(sensorDataList);

        return FlightDataResponse.builder()
                .flight(flightService.getFlightById(flight.getId()).orElse(null))
                .sensorData(sensorDataDTOs)
                .summary(summary)
                .build();
    }

    private List<SensorData> sampleData(List<SensorData> data, int maxPoints) {
        if (data.size() <= maxPoints) {
            return data;
        }

        List<SensorData> sampled = new ArrayList<>();
        double step = (double) data.size() / maxPoints;

        for (int i = 0; i < maxPoints; i++) {
            int index = (int) (i * step);
            if (index < data.size()) {
                sampled.add(data.get(index));
            }
        }

        if (sampled.get(sampled.size() - 1) != data.get(data.size() - 1)) {
            sampled.add(data.get(data.size() - 1));
        }

        return sampled;
    }

    private SensorSummaryDTO calculateSummary(List<SensorData> data) {
        if (data.isEmpty()) {
            return SensorSummaryDTO.builder()
                    .dataPointCount(0L)
                    .duration("0秒")
                    .build();
        }

        DoubleSummaryStatistics altitudeStats = data.stream()
                .filter(d -> d.getAltitude() != null)
                .mapToDouble(SensorData::getAltitude)
                .summaryStatistics();

        DoubleSummaryStatistics speedStats = data.stream()
                .filter(d -> d.getSpeed() != null)
                .mapToDouble(SensorData::getSpeed)
                .summaryStatistics();

        DoubleSummaryStatistics engineTempStats = data.stream()
                .filter(d -> d.getEngineTemperature() != null)
                .mapToDouble(SensorData::getEngineTemperature)
                .summaryStatistics();

        double totalFuel = data.stream()
                .filter(d -> d.getFuelConsumption() != null)
                .mapToDouble(SensorData::getFuelConsumption)
                .sum();

        LocalDateTime firstTime = data.get(0).getTimestamp();
        LocalDateTime lastTime = data.get(data.size() - 1).getTimestamp();
        Duration duration = Duration.between(firstTime, lastTime);

        String durationStr = formatDuration(duration);

        return SensorSummaryDTO.builder()
                .minAltitude(altitudeStats.getCount() > 0 ? altitudeStats.getMin() : null)
                .maxAltitude(altitudeStats.getCount() > 0 ? altitudeStats.getMax() : null)
                .avgAltitude(altitudeStats.getCount() > 0 ? altitudeStats.getAverage() : null)
                .minSpeed(speedStats.getCount() > 0 ? speedStats.getMin() : null)
                .maxSpeed(speedStats.getCount() > 0 ? speedStats.getMax() : null)
                .avgSpeed(speedStats.getCount() > 0 ? speedStats.getAverage() : null)
                .minEngineTemp(engineTempStats.getCount() > 0 ? engineTempStats.getMin() : null)
                .maxEngineTemp(engineTempStats.getCount() > 0 ? engineTempStats.getMax() : null)
                .avgEngineTemp(engineTempStats.getCount() > 0 ? engineTempStats.getAverage() : null)
                .totalFuelConsumption(totalFuel)
                .dataPointCount((long) data.size())
                .duration(durationStr)
                .build();
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    @Transactional
    public void deleteSensorDataByFlightId(Long flightId) {
        sensorDataRepository.deleteByFlightId(flightId);
    }

    private SensorDataDTO toDTO(SensorData data) {
        return SensorDataDTO.builder()
                .id(data.getId())
                .timestamp(data.getTimestamp())
                .altitude(data.getAltitude())
                .speed(data.getSpeed())
                .engineTemperature(data.getEngineTemperature())
                .fuelConsumption(data.getFuelConsumption())
                .cabinPressure(data.getCabinPressure())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .verticalSpeed(data.getVerticalSpeed())
                .heading(data.getHeading())
                .build();
    }
}
