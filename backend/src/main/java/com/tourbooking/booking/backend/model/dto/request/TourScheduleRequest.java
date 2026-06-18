package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TourScheduleRequest {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime departureTime;
    private LocalTime returnTime;
    private Integer availableSlots;
    private Integer maxSlots;
    /**
     * Optional: explicit booking deadline. If null, defaults to departure datetime on the backend.
     */
    private LocalDateTime bookingDeadline;
}
