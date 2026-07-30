package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuspendScheduleRequest {
    @NotNull
    private Long scheduleId;

    @NotNull
    private com.tourbooking.booking.backend.model.entity.enums.SuspensionReasonType suspensionReasonType;

    @NotNull
    private String suspensionReason;

    private java.time.LocalDate suspendedFrom;
    private java.time.LocalDate suspendedUntil;
}
