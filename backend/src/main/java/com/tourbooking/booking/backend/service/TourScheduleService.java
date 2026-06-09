package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.TourSchedule;

public interface TourScheduleService {

    /**
     * Trừ chỗ trống với Pessimistic Lock. Chỉ ADULT + CHILD chiếm chỗ (INFANT không tính).
     */
    TourSchedule deductAvailableSlots(Long scheduleId, int slotsToDeduct);

    void releaseAvailableSlots(Long scheduleId, int slotsToRelease);
}
