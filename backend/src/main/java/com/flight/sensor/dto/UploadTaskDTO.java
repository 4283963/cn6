package com.flight.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTaskDTO {
    private String taskId;
    private String flightNumber;
    private String fileName;
    private Long fileSize;
    private String status;
    private Long processedRecords;
    private Long totalRecords;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer progress;
}
