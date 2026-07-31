package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class SuspendPreviewResponse {
    private Long scheduleId;
    private LocalDate startDate;
    private String departureTime;
    private int affectedBookingCount;
    private String currentStatus;
}
