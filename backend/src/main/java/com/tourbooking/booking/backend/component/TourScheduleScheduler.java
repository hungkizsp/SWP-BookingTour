package com.tourbooking.booking.backend.component;

import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TourScheduleScheduler — Background Jobs for the Guaranteed Departure Model.
 *
 * <ul>
 *   <li>Every 5 minutes: OPEN/SOLD_OUT schedules past their booking deadline → BOOKING_CLOSED</li>
 *   <li>Every 5 minutes: OPEN/BOOKING_CLOSED/SOLD_OUT schedules past departure → IN_PROGRESS</li>
 *   <li>Every 5 minutes: IN_PROGRESS schedules past return → COMPLETED</li>
 * </ul>
 *
 * NOTE: "Guaranteed Departure" — a tour runs regardless of participant count.
 * There is NO minimum-participant cancellation logic here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourScheduleScheduler {

    private final TourScheduleRepository tourScheduleRepository;

    // ── Job 1: Close Booking Deadline ────────────────────────────────────────────
    /**
     * Every 5 minutes: find OPEN or SOLD_OUT schedules whose booking deadline has
     * passed and transition them to BOOKING_CLOSED.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void closeExpiredBookingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourStatus> eligibleStatuses = List.of(TourStatus.OPEN, TourStatus.SOLD_OUT);

        List<TourSchedule> schedules = tourScheduleRepository
                .findSchedulesPastDeadline(eligibleStatuses, now, today);

        if (schedules.isEmpty()) {
            return;
        }

        int updated = 0;
        for (TourSchedule schedule : schedules) {
            // Double-check the effective deadline at entity level to handle null departureTime
            LocalDateTime effectiveDeadline = schedule.getEffectiveBookingDeadline();
            if (effectiveDeadline == null || now.isBefore(effectiveDeadline)) {
                continue; // Not yet expired (null startDate edge case)
            }

            TourStatus old = schedule.getStatus();
            schedule.setStatus(TourStatus.BOOKING_CLOSED);
            tourScheduleRepository.save(schedule);
            updated++;
            log.info("[SCHEDULER] Schedule #{} (was {}) → BOOKING_CLOSED at deadline={}",
                    schedule.getId(), old, effectiveDeadline);
        }

        if (updated > 0) {
            log.info("[SCHEDULER] closeExpiredBookingDeadlines: {} schedule(s) → BOOKING_CLOSED", updated);
        }
    }

    // ── Job 2: Transition to IN_PROGRESS at Departure ────────────────────────────
    /**
     * Every 5 minutes: find schedules that should now be IN_PROGRESS
     * (departure datetime has arrived or passed) and were in OPEN, BOOKING_CLOSED, or SOLD_OUT.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void markInProgressAtDeparture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Pre-filter by date at DB level; then do precise time check at entity level
        List<TourStatus> eligibleStatuses = List.of(
                TourStatus.OPEN, TourStatus.BOOKING_CLOSED, TourStatus.SOLD_OUT);

        List<TourSchedule> candidates = tourScheduleRepository
                .findSchedulesPastDeparture(eligibleStatuses, today);

        if (candidates.isEmpty()) {
            return;
        }

        int updated = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departureDateTime = schedule.getDepartureDateTime();
            if (departureDateTime == null || now.isBefore(departureDateTime)) {
                continue; // departureTime pushes it later in the day
            }

            // ── GUIDE REQUIREMENT GUARD ───────────────────────────────────────
            // A schedule CANNOT go IN_PROGRESS without an assigned guide.
            // The OperationalScheduler will transition it to PENDING_GUIDE / CANCELLED_BY_OPERATOR.
            if (schedule.getGuide() == null) {
                log.warn("[SCHEDULER] Schedule #{} reached departure but has NO guide — skipping IN_PROGRESS transition.",
                        schedule.getId());
                continue;
            }

            TourStatus old = schedule.getStatus();
            schedule.setStatus(TourStatus.IN_PROGRESS);
            tourScheduleRepository.save(schedule);
            updated++;
            log.info("[SCHEDULER] Schedule #{} (was {}) → IN_PROGRESS at departure={}",
                    schedule.getId(), old, departureDateTime);
        }

        if (updated > 0) {
            log.info("[SCHEDULER] markInProgressAtDeparture: {} schedule(s) → IN_PROGRESS", updated);
        }
    }

    // ── Job 3: Transition to COMPLETED at Return ──────────────────────────────────
    /**
     * Every 5 minutes: find IN_PROGRESS schedules whose return datetime has passed
     * and transition them to COMPLETED.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void markCompletedAtReturn() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourSchedule> candidates = tourScheduleRepository.findSchedulesPastReturn(today);

        if (candidates.isEmpty()) {
            return;
        }

        int updated = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime returnDateTime = schedule.getReturnDateTime();
            if (returnDateTime == null || now.isBefore(returnDateTime)) {
                continue; // returnTime pushes it later in the day
            }

            schedule.setStatus(TourStatus.COMPLETED);
            tourScheduleRepository.save(schedule);
            updated++;
            log.info("[SCHEDULER] Schedule #{} → COMPLETED at return={}",
                    schedule.getId(), returnDateTime);
        }

        if (updated > 0) {
            log.info("[SCHEDULER] markCompletedAtReturn: {} schedule(s) → COMPLETED", updated);
        }
    }
}
