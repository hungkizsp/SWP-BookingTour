package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TourScheduleRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer availableSlots;
    private Integer maxSlots;
}
