package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.dto.request.TourScheduleBulkRequest;
import java.util.List;

public interface TourScheduleService {

    /**
     * Trừ chỗ trống với Pessimistic Lock. Chỉ ADULT + CHILD chiếm chỗ (INFANT không tính).
     */
    TourSchedule deductAvailableSlots(Long scheduleId, int slotsToDeduct);

    void releaseAvailableSlots(Long scheduleId, int slotsToRelease);

    List<TourSchedule> bulkCreateSchedules(TourScheduleBulkRequest request);

    void cancelTourSchedule(Long scheduleId);

    /**
     * Releases the assigned guide when no active bookings remain on the schedule.
     */
    void releaseGuideIfNoActiveBookings(Long scheduleId);
}
