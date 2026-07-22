package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleCandidateResponse {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime departureTime;
    private Integer availableSlots;
    private String status;
}
