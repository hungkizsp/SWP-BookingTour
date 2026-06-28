package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Statistics for booking history page
 * Used in UC18: View Booking History
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatistics {
    private long totalBookings;
    private long confirmedBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long completedBookings;
    private BigDecimal totalSpent;
}
