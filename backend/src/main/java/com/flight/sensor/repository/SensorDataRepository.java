package com.flight.sensor.repository;

import com.flight.sensor.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    List<SensorData> findByFlightIdOrderByTimestampAsc(Long flightId);

    @Query("SELECT s FROM SensorData s WHERE s.flightId = :flightId ORDER BY s.timestamp ASC")
    List<SensorData> findAllByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT s FROM SensorData s WHERE s.flightId = :flightId AND s.timestamp BETWEEN :start AND :end ORDER BY s.timestamp ASC")
    List<SensorData> findByFlightIdAndTimestampBetween(
            @Param("flightId") Long flightId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Long countByFlightId(Long flightId);

    void deleteByFlightId(Long flightId);

    @Query(value = "SELECT * FROM sensor_data WHERE flight_id = :flightId ORDER BY timestamp ASC", nativeQuery = true)
    List<SensorDataStream> findByFlightIdStream(@Param("flightId") Long flightId);

    interface SensorDataStream {
        Long getId();
        Long getFlightId();
        java.time.LocalDateTime getTimestamp();
        Double getAltitude();
        Double getSpeed();
        Double getEngineTemperature();
        Double getFuelConsumption();
        Double getCabinPressure();
        Double getLatitude();
        Double getLongitude();
        Double getVerticalSpeed();
        Double getHeading();
    }
}
