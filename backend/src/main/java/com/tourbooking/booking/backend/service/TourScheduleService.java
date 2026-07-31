package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.dto.request.TourScheduleBulkRequest;
import com.tourbooking.booking.backend.model.dto.request.SuspendTourRequest;
import com.tourbooking.booking.backend.model.dto.response.SuspendPreviewResponse;
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

    // ── New date-range suspension API ────────────────────────────────────────

    /**
     * Preview schedules falling in [from, until] for the given tour.
     * Returns a lightweight list so admin can deselect specific ones before submitting.
     */
    List<SuspendPreviewResponse> previewSuspension(Long tourId, java.time.LocalDate from, java.time.LocalDate until);

    /**
     * Suspend a tour by date range:
     * - Suspends only schedules whose id is in request.includedScheduleIds (all if null/empty means all in range).
     * - Sets SUSPENDED + stores reason/type/dates on each affected schedule.
     * - Marks active bookings PENDING_CUSTOMER_ACTION.
     */
    void suspendTourByDateRange(Long tourId, SuspendTourRequest request);

    /**
     * Resume ALL suspended schedules of a tour (admin "Mở lại"):
     * - Sets status OPEN, clears suspension fields.
     */
    void resumeTour(Long tourId);

    /** Return active bookings for a schedule (for preview before suspend). */
    java.util.List<com.tourbooking.booking.backend.model.entity.Booking> getAffectedBookings(Long scheduleId);
}
