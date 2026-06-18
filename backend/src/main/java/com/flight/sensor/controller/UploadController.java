package com.flight.sensor.controller;

import com.flight.sensor.dto.ApiResponse;
import com.flight.sensor.dto.UploadTaskDTO;
import com.flight.sensor.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadTaskDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("flightNumber") String flightNumber) {

        log.info("收到文件上传请求: flightNumber={}, fileName={}, fileSize={}",
                flightNumber, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ApiResponse.error("上传文件不能为空");
        }

        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            return ApiResponse.error("航班号不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ApiResponse.error("只支持CSV格式文件");
        }

        try {
            UploadTaskDTO task = uploadService.createUploadTask(file, flightNumber);
            return ApiResponse.success("文件上传成功，正在后台解析处理", task);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/progress/{taskId}")
    public ApiResponse<UploadTaskDTO> getProgress(@PathVariable String taskId) {
        UploadTaskDTO task = uploadService.getTaskProgress(taskId);
        if (task == null) {
            return ApiResponse.error("任务不存在");
        }
        return ApiResponse.success(task);
    }

    @GetMapping("/tasks")
    public ApiResponse<List<UploadTaskDTO>> getAllTasks() {
        return ApiResponse.success(uploadService.getAllTasks());
    }
}
