package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TourScheduleBulkRequest {
    private Long tourId;
    private LocalTime departureTime;
    private Integer maxSlots;
    private BigDecimal price; // Will be ignored if schedule doesn't use it, but keeping for compatibility
    private LocalDate rangeStartDate; 
    private LocalDate rangeEndDate;   
    private List<DayOfWeek> daysOfWeek;
}
