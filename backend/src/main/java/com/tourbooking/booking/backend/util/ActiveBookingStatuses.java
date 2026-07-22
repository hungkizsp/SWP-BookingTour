package com.tourbooking.booking.backend.util;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;

import java.util.List;

public final class ActiveBookingStatuses {

    /** Bookings that occupy a schedule slot or require guide/operational handling. */
    public static final List<BookingStatus> STATUSES = List.of(
            BookingStatus.CONFIRMED,
            BookingStatus.PAID,
            BookingStatus.PENDING,
            BookingStatus.PENDING_CASH);

    private ActiveBookingStatuses() {
    }
}
