package com.tourbooking.booking.backend.model.entity.enums;

/**
 * Customer-facing action status for bookings affected by a tour suspension.
 * <ul>
 *   <li>PENDING_CUSTOMER_ACTION – customer has not yet chosen reschedule or refund</li>
 *   <li>RESOLVED – customer chose an action (or admin resolved it)</li>
 * </ul>
 */
public enum SuspensionActionStatus {
    PENDING_CUSTOMER_ACTION,
    RESOLVED
}
