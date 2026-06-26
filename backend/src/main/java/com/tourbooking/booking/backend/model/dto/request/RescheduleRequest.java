package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RescheduleRequest {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "New Schedule ID is required")
    private Long newScheduleId;
}
