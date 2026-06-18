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
public class SensorDataDTO {
    private Long id;
    private LocalDateTime timestamp;
    private Double altitude;
    private Double speed;
    private Double engineTemperature;
    private Double fuelConsumption;
    private Double cabinPressure;
    private Double latitude;
    private Double longitude;
    private Double verticalSpeed;
    private Double heading;
}
