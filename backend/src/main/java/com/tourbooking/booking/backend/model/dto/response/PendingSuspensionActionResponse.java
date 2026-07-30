package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PendingSuspensionActionResponse {
    private Long bookingId;
    private Long scheduleId;
    private String tourName;
    private LocalDate departureDate;
    private BigDecimal totalPrice;
    private com.tourbooking.booking.backend.model.entity.enums.SuspensionReasonType suspensionReasonType;
    private String suspensionReason;
    private LocalDate suspendedFrom;
    private LocalDate suspendedUntil;
    /** true if there is at least one candidate schedule within the reschedule window. */
    private boolean canReschedule;
}
