package com.flight.sensor.controller;

import com.flight.sensor.dto.ApiResponse;
import com.flight.sensor.dto.FlightDTO;
import com.flight.sensor.dto.FlightDataResponse;
import com.flight.sensor.service.FlightService;
import com.flight.sensor.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final SensorDataService sensorDataService;

    @GetMapping
    public ApiResponse<List<FlightDTO>> getAllFlights(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(flightService.searchFlights(keyword));
    }

    @GetMapping("/numbers")
    public ApiResponse<List<String>> getAllFlightNumbers() {
        return ApiResponse.success(flightService.getAllFlightNumbers());
    }

    @GetMapping("/{flightNumber}")
    public ApiResponse<FlightDTO> getFlightByNumber(@PathVariable String flightNumber) {
        return flightService.getFlightByFlightNumber(flightNumber)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("航班不存在"));
    }

    @GetMapping("/{flightNumber}/data")
    public ApiResponse<FlightDataResponse> getFlightData(
            @PathVariable String flightNumber,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.debug("查询航班数据: flightNumber={}, maxPoints={}, startTime={}, endTime={}",
                flightNumber, maxPoints, startTime, endTime);

        FlightDataResponse response;

        if (startTime != null || endTime != null) {
            response = sensorDataService.getFlightDataByFlightNumberWithTimeRange(
                    flightNumber, startTime, endTime);
        } else if (maxPoints != null && maxPoints > 0) {
            response = sensorDataService.getFlightDataByFlightNumberWithSampling(
                    flightNumber, maxPoints);
        } else {
            response = sensorDataService.getFlightDataByFlightNumber(flightNumber);
        }

        if (response == null) {
            return ApiResponse.error("航班数据不存在");
        }

        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFlight(@PathVariable Long id) {
        sensorDataService.deleteSensorDataByFlightId(id);
        flightService.deleteFlight(id);
        return ApiResponse.success("删除成功", null);
    }
}
