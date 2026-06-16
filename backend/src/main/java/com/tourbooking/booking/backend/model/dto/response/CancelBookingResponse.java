package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response after cancelling a booking
 * Used in UC20: Cancel Booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponse {
    private boolean success;
    private String message;
    private String bookingReference;
    private LocalDateTime cancellationTimestamp;
    private boolean refundEligible;
    private String refundMessage;
}
