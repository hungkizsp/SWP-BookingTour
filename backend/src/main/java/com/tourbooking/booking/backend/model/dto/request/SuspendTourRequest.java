package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SuspendTourRequest {
    @NotNull
    private com.tourbooking.booking.backend.model.entity.enums.SuspensionReasonType suspensionReasonType;

    @NotNull
    private String suspensionReason;

    @NotNull
    private LocalDate suspendedFrom;

    @NotNull
    private LocalDate suspendedUntil;

    private List<Long> includedScheduleIds;
}
