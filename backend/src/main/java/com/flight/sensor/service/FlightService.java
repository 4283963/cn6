package com.flight.sensor.service;

import com.flight.sensor.dto.FlightDTO;
import com.flight.sensor.entity.Flight;
import com.flight.sensor.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    public List<FlightDTO> getAllFlights() {
        return flightRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<FlightDTO> searchFlights(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllFlights();
        }
        return flightRepository.findByFlightNumberContainingIgnoreCaseOrderByCreatedAtDesc(keyword).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<FlightDTO> getFlightByFlightNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber).map(this::toDTO);
    }

    public Optional<FlightDTO> getFlightById(Long id) {
        return flightRepository.findById(id).map(this::toDTO);
    }

    public List<String> getAllFlightNumbers() {
        return flightRepository.findAllFlightNumbers();
    }

    @Transactional
    public FlightDTO createFlight(Flight flight) {
        Flight saved = flightRepository.save(flight);
        return toDTO(saved);
    }

    @Transactional
    public void deleteFlight(Long id) {
        flightRepository.deleteById(id);
    }

    private FlightDTO toDTO(Flight flight) {
        return FlightDTO.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .departure(flight.getDeparture())
                .arrival(flight.getArrival())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .aircraftType(flight.getAircraftType())
                .aircraftRegistration(flight.getAircraftRegistration())
                .description(flight.getDescription())
                .totalRecords(flight.getTotalRecords())
                .createdAt(flight.getCreatedAt())
                .build();
    }
}
