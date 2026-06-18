package com.flight.sensor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sensor_data", indexes = {
        @Index(name = "idx_sensor_flight_id", columnList = "flightId"),
        @Index(name = "idx_sensor_timestamp", columnList = "timestamp")
})
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false)
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
