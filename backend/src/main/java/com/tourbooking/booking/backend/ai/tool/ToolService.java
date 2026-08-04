package com.tourbooking.booking.backend.ai.tool;

import java.time.LocalDate;

public interface ToolService {

    /** Retrieve real-time or simulated weather for given location and dates */
    WeatherInfo getWeather(String location, LocalDate from, LocalDate to);

    /** Fetch human-readable tour context */
    String buildTourContext(Long tourId);

    /** Fetch human-readable user booking history */
    String getUserBookingSummary(Long userId);
}
