package com.flight.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorSummaryDTO {
    private Double minAltitude;
    private Double maxAltitude;
    private Double avgAltitude;

    private Double minSpeed;
    private Double maxSpeed;
    private Double avgSpeed;

    private Double minEngineTemp;
    private Double maxEngineTemp;
    private Double avgEngineTemp;

    private Double totalFuelConsumption;

    private Long dataPointCount;
    private String duration;
}
