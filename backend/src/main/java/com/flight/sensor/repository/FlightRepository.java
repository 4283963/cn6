package com.flight.sensor.repository;

import com.flight.sensor.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByFlightNumberContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<Flight> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT f.flightNumber FROM Flight f ORDER BY f.flightNumber")
    List<String> findAllFlightNumbers();
}
