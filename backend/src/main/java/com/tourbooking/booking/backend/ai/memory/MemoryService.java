package com.tourbooking.booking.backend.ai.memory;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.UserAIProfile;

public interface MemoryService {

    /** Retrieve AI Memory profile for given user */
    UserAIProfile getProfile(Long userId);

    /** Update memory metrics asynchronously after booking completion */
    void updateAfterBooking(Long userId, Booking booking);

    /** Get human-readable memory summary string for prompt context */
    String getProfileSummary(Long userId);
}
