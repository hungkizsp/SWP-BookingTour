package com.tourbooking.booking.backend.model.entity.enums;

/**
 * Schedule-level status for TourSchedule (Guaranteed Departure Model).
 * <ul>
 *   <li>OPEN – accepting bookings</li>
 *   <li>BOOKING_CLOSED – booking deadline has passed, tour still pending departure</li>
 *   <li>SOLD_OUT – no available slots left</li>
 *   <li>PENDING_GUIDE – departure &lt; 1 hour away but no guide assigned; new bookings blocked</li>
 *   <li>IN_PROGRESS – tour has departed (requires guideId != null)</li>
 *   <li>COMPLETED – tour has returned</li>
 *   <li>CANCELLED – manually cancelled by admin</li>
 *   <li>CANCELLED_BY_OPERATOR – auto-cancelled because no guide was assigned by departure time</li>
 * </ul>
 */
public enum TourStatus {
    OPEN,
    BOOKING_CLOSED,
    SOLD_OUT,
    PENDING_GUIDE,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    CANCELLED_BY_OPERATOR
}
