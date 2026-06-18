package com.flight.sensor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FlightSensorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightSensorApplication.class, args);
    }
}
