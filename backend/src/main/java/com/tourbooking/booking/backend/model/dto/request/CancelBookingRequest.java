package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to cancel a booking
 * Used in UC20: Cancel Booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
    
    private String additionalDetails;
}
