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
@Table(name = "flights", indexes = {
        @Index(name = "idx_flight_number", columnList = "flightNumber")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String flightNumber;

    @Column(length = 100)
    private String departure;

    @Column(length = 100)
    private String arrival;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    @Column(length = 100)
    private String aircraftType;

    @Column(length = 100)
    private String aircraftRegistration;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long totalRecords;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
