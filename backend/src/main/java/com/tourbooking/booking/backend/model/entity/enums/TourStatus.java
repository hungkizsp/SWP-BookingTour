package com.tourbooking.booking.backend.model.entity.enums;

/**
 * Schedule-level status for TourSchedule (Guaranteed Departure Model).
 * <ul>
 *   <li>OPEN – accepting bookings</li>
 *   <li>BOOKING_CLOSED – booking deadline has passed, tour still pending departure</li>
 *   <li>SOLD_OUT – no available slots left</li>
 *   <li>IN_PROGRESS – tour has departed</li>
 *   <li>COMPLETED – tour has returned</li>
 *   <li>CANCELLED – manually cancelled by admin</li>
 * </ul>
 */
public enum TourStatus {
    OPEN,
    BOOKING_CLOSED,
    SOLD_OUT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
