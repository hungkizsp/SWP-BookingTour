package com.tourbooking.booking.backend.component;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.OperationalAlert;
import com.tourbooking.booking.backend.model.entity.Payment;
import com.tourbooking.booking.backend.model.entity.PaymentLog;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.OperationalAlertRepository;
import com.tourbooking.booking.backend.repository.PaymentLogRepository;
import com.tourbooking.booking.backend.repository.PaymentRepository;
import com.tourbooking.booking.backend.repository.RefundRequestRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OperationalScheduler — Guide Readiness Enforcement for the Guaranteed Departure Model.
 *
 * <ul>
 *   <li>Every 3 minutes: Early-Warning alerts (24H/12H/6H/2H) for OPEN schedules with no guide.</li>
 *   <li>Every 3 minutes: Hard-Stop — transition OPEN (no guide, &lt;1h before departure) → PENDING_GUIDE.</li>
 *   <li>Every 3 minutes: Auto-Cancel — transition PENDING_GUIDE past departure → CANCELLED_BY_OPERATOR
 *       and process full refunds for all CONFIRMED bookings.</li>
 *   <li>Patches the existing IN_PROGRESS auto-start: only allows it if guide is assigned.</li>
 * </ul>
 *
 * <p>All jobs are idempotent: they check current state before acting and skip already-processed items.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationalScheduler {

    // ─── Alert window definitions (ordered most-to-least relaxed) ─────────────
    // IMPORTANT: must remain in descending-hour order (24 → 2) because the loop
    // fires the FIRST window whose threshold has been entered. Once a more urgent
    // (smaller-hour) window has already been recorded in the DB for a schedule,
    // any less-urgent windows are skipped via skipBelowIndex (see sendEarlyWarningAlerts).
    private static final long[]   ALERT_HOURS   = {24L, 12L, 6L, 2L};
    private static final String[] ALERT_WINDOWS = {"24H", "12H", "6H", "2H"};

    private final TourScheduleRepository tourScheduleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final OperationalAlertRepository operationalAlertRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final MailService mailService;

    // ══════════════════════════════════════════════════════════════════════
    // JOB 1 — Early-Warning Alerts (24H / 12H / 6H / 2H before departure)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs every 3 minutes. Scans upcoming OPEN schedules without a guide and fires
     * one-time notifications for each threshold window that has been entered.
     *
     * <p><b>Duplicate-alert fix (break):</b> After successfully recording and firing one NEW alert
     * window for a schedule, we {@code break} out of the inner loop. This prevents a schedule
     * that is detected late (e.g. its departure already crossed both the 24H and 12H thresholds
     * simultaneously) from generating multiple alert cards in a single scheduler run. Subsequent
     * runs will pick up the next threshold if the guide is still missing.</p>
     *
     * <p><b>Past-departure guard:</b> Schedules whose departure has already passed are skipped;
     * the auto-cancel job (Job 3) is responsible for handling them.</p>
     */
    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void sendEarlyWarningAlerts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourSchedule> candidates = tourScheduleRepository.findEligibleSchedulesWithNoGuide(today);

        int alertsSent = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null) continue;

            // Skip schedules whose departure has already passed - Job 3 (auto-cancel) handles those.
            if (now.isAfter(departure)) {
                continue;
            }

            // Bypass workflows if schedule has 0 valid bookings
            long validBookings = bookingRepository.countValidBookingsByScheduleId(schedule.getId());
            if (validBookings <= 0) {
                continue;
            }

            // ── Determine the most urgent window already fired for this schedule ──────
            // If e.g. "12H" has already been sent, we must NOT send "24H" even if it
            // hasn't been stored yet (happens when a schedule is detected AFTER it
            // crossed both the 24H and 12H thresholds in the same scheduler cycle).
            // We find the index of the most-urgent (smallest hours) existing window.
            int mostUrgentExistingIdx = -1;
            for (int i = ALERT_WINDOWS.length - 1; i >= 0; i--) {
                if (operationalAlertRepository.existsByScheduleIdAndAlertWindow(schedule.getId(), ALERT_WINDOWS[i])) {
                    mostUrgentExistingIdx = i;
                    break;
                }
            }

            // FIX Bug B: Iterate backwards from most urgent (2H) to least urgent (24H).
            // If a schedule is created at 10 hours remaining, we want to fire ONLY 12H (and break).
            // If we iterated 24H -> 2H, we would incorrectly fire 24H first because it's valid, 
            // and miss firing 12H until the next run (causing a duplicate).
            for (int i = ALERT_HOURS.length - 1; i >= 0; i--) {
                long hoursAhead = ALERT_HOURS[i];
                String window = ALERT_WINDOWS[i];

                // Check: are we inside (or past) this window?
                LocalDateTime windowStart = departure.minusHours(hoursAhead);
                if (now.isBefore(windowStart)) {
                    continue; // Not yet in this window — skip
                }

                // ── Skip less-urgent windows superseded by a more urgent existing one ──
                // Example: if "12H" (index 1) already exists, skip "24H" (index 0) even
                // if it was never stored — it is no longer actionable information.
                if (mostUrgentExistingIdx > i) {
                    // A more urgent window (smaller hours, higher index) already fired.
                    // This less-urgent window is now irrelevant — don't backfill it.
                    log.debug("[OPS-ALERT] Skipping {} for schedule #{} — {} already sent (more urgent)",
                            window, schedule.getId(), ALERT_WINDOWS[mostUrgentExistingIdx]);
                    continue;
                }

                // Idempotency: has this exact alert already been sent?
                if (operationalAlertRepository.existsByScheduleIdAndAlertWindow(schedule.getId(), window)) {
                    continue;
                }

                // Persist the alert record (unique constraint prevents duplicates from race conditions)
                try {
                    OperationalAlert alert = OperationalAlert.builder()
                            .scheduleId(schedule.getId())
                            .alertWindow(window)
                            .build();
                    operationalAlertRepository.saveAndFlush(alert);
                } catch (Exception e) {
                    log.debug("[OPS-ALERT] Race-condition duplicate suppressed for schedule #{} window {}", schedule.getId(), window);
                    continue;
                }

                // Fire admin notification via mail (if configured)
                String tourName = schedule.getTour() != null ? schedule.getTour().getTourName() : "Tour #" + schedule.getId();
                String subject = "[TourBooking] 🚨 URGENT: No Guide Assigned — " + window + " before departure";
                String body = "Schedule #" + schedule.getId() + " (" + tourName + ") departs at " + departure +
                        " but has NO guide assigned.\n\n" +
                        "⚠️  Action required: Please assign a guide immediately.\n\n" +
                        "Scheduled departure: " + departure + "\n" +
                        "Alert window: " + window + " before departure";

                log.warn("[OPS-ALERT] {} alert for Schedule #{} — departure at {}", window, schedule.getId(), departure);

                // Try to send email to admin (best-effort, non-blocking)
                try {
                    mailService.sendOperationalAlert(subject, body);
                } catch (Exception e) {
                    log.warn("[OPS-ALERT] Could not send alert email for schedule #{}: {}", schedule.getId(), e.getMessage());
                }

                alertsSent++;

                // Break after firing one new alert per schedule per run.
                // Combined with the mostUrgentExistingIdx check above, this ensures:
                // – At most 1 alert fires per schedule per run.
                // – A schedule that crosses multiple thresholds in one cycle only gets
                //   the most urgent (smallest remaining time) alert that is applicable.
                break;
            }
        }

        if (alertsSent > 0) {
            log.info("[OPS-ALERT] Sent {} early-warning alert(s) for schedules missing guides.", alertsSent);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // JOB 2 — Hard Stop: OPEN → PENDING_GUIDE (< 1 hour before departure)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs every 3 minutes. OPEN schedules with no guide that are within 1 hour of
     * departure are transitioned to PENDING_GUIDE, blocking new bookings.
     */
    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void applyGuideHardStop() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourSchedule> candidates = tourScheduleRepository.findEligibleNoGuideSchedulesOnOrBeforeDate(today);

        int transitioned = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null) continue;

            // Bypass workflows if schedule has 0 valid bookings
            long validBookings = bookingRepository.countValidBookingsByScheduleId(schedule.getId());
            if (validBookings <= 0) {
                continue;
            }

            // Only flag if we are within 1 hour of departure (or past it but not yet handled by auto-cancel)
            if (now.isBefore(departure.minusHours(1))) {
                continue;
            }

            if (now.isAfter(departure)) {
                // Past departure — the auto-cancel job will handle this
                continue;
            }

            // CRITICAL GUARD: Only auto-transition OPEN, BOOKING_CLOSED, SOLD_OUT schedules to PENDING_GUIDE.
            if (schedule.getStatus() != TourStatus.OPEN && schedule.getStatus() != TourStatus.BOOKING_CLOSED && schedule.getStatus() != TourStatus.SOLD_OUT) {
                continue;
            }

            log.warn("[OPS-HARDSTOP] Schedule #{} → PENDING_GUIDE (departure={}, now={})",
                    schedule.getId(), departure, now);
            schedule.setStatus(TourStatus.PENDING_GUIDE);
            tourScheduleRepository.save(schedule);
            transitioned++;
        }

        if (transitioned > 0) {
            log.info("[OPS-HARDSTOP] {} schedule(s) transitioned to PENDING_GUIDE.", transitioned);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // JOB 3 — Auto-Cancellation & Auto-Refund at Departure Time
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs every 3 minutes. PENDING_GUIDE schedules whose departure has arrived are
     * automatically cancelled and all CONFIRMED bookings receive a full refund.
     */
    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void autoCancelAndRefundPendingGuideSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourSchedule> candidates = tourScheduleRepository.findPendingGuideSchedulesPastDeparture(today);

        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null || now.isBefore(departure)) {
                continue; // departure time not yet reached
            }

            log.warn("[OPS-CANCEL] Auto-cancelling Schedule #{} (PENDING_GUIDE past departure={}).",
                    schedule.getId(), departure);

            // Step A: Cancel the schedule
            schedule.setStatus(TourStatus.CANCELLED_BY_OPERATOR);
            tourScheduleRepository.save(schedule);

            // Step B: Set all CONFIRMED bookings to REFUND_REQUESTED
            List<Booking> confirmedBookings = bookingRepository.findConfirmedByScheduleId(schedule.getId());
            for (Booking booking : confirmedBookings) {
                processOperatorCancellationToRefundRequest(booking, schedule);
            }

            log.info("[OPS-CANCEL] Schedule #{} cancelled. {} booking(s) moved to REFUND_REQUESTED.", schedule.getId(), confirmedBookings.size());
        }
    }

    /**
     * Processes a cancellation for a single booking by setting it to REFUND_REQUESTED,
     * creating a RefundRequest, and notifying the customer immediately.
     */
    private void processOperatorCancellationToRefundRequest(Booking booking, TourSchedule schedule) {
        try {
            BookingStatus originalStatus = booking.getStatus();
            // 1. Set booking to REFUND_REQUESTED
            booking.setStatus(BookingStatus.REFUND_REQUESTED);
            bookingRepository.save(booking);

            // 2. Create RefundRequest
            com.tourbooking.booking.backend.model.entity.RefundRequest refundRequest = new com.tourbooking.booking.backend.model.entity.RefundRequest();
            refundRequest.setBooking(booking);
            refundRequest.setAmount(booking.getTotalPrice());
            refundRequest.setReason("[HỆ THỐNG] Tour bị hủy tự động do không có Guide. Cần hoàn tiền 100% cho khách.");
            refundRequest.setStatus(com.tourbooking.booking.backend.model.entity.enums.RefundStatus.PENDING);
            refundRequest.setOriginalBookingStatus(originalStatus);
            refundRequestRepository.save(refundRequest);

            // 3. Send customer notification email
            if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                try {
                    String tourName = schedule.getTour() != null ? schedule.getTour().getTourName() : "N/A";
                    mailService.sendTourCancellationEmail(
                            booking.getUser().getEmail(),
                            booking.getUser().getFullName(),
                            booking.getId(),
                            tourName,
                            "Yêu cầu vận hành không được đáp ứng (Không có Hướng dẫn viên). Chúng tôi đang tiến hành hoàn tiền 100% cho bạn."
                    );
                } catch (Exception emailEx) {
                    log.warn("[OPS-CANCEL] Failed to send cancellation email for booking #{}: {}",
                            booking.getId(), emailEx.getMessage());
                }
            }

            log.info("[OPS-CANCEL] Booking #{} moved to REFUND_REQUESTED (GUIDE_NOT_ASSIGNED).", booking.getId());
        } catch (Exception ex) {
            log.error("[OPS-CANCEL] Error processing refund request for booking #{}: {}", booking.getId(), ex.getMessage(), ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // JOB 4 — Patched: Auto-Start requires a guide (IN_PROGRESS guard)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Companion to TourScheduleScheduler.markInProgressAtDeparture().
     * This job validates that any schedule about to become IN_PROGRESS has a guide.
     * If not, it transitions to PENDING_GUIDE instead.
     * <p>
     * Note: TourScheduleScheduler is kept as-is; this job prevents OPEN/BOOKING_CLOSED/SOLD_OUT
     * schedules WITHOUT a guide from going IN_PROGRESS by intercepting them first.
     * </p>
     */
    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void enforceGuideRequirementForInProgress() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Find OPEN schedules with no guide that have reached or passed departure (not yet > 1 day old)
        List<TourSchedule> candidatesForProgress = tourScheduleRepository
                .findEligibleNoGuideSchedulesOnOrBeforeDate(today);

        for (TourSchedule schedule : candidatesForProgress) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null || now.isBefore(departure)) {
                continue; // not yet at departure
            }

            // Bypass workflows if schedule has 0 valid bookings
            long validBookings = bookingRepository.countValidBookingsByScheduleId(schedule.getId());
            if (validBookings <= 0) {
                continue;
            }
            // CRITICAL GUARD: Only auto-transition OPEN, BOOKING_CLOSED, SOLD_OUT schedules to PENDING_GUIDE.
            if (schedule.getStatus() == TourStatus.OPEN || schedule.getStatus() == TourStatus.BOOKING_CLOSED || schedule.getStatus() == TourStatus.SOLD_OUT) {
                log.warn("[OPS-GUARD] Schedule #{} reached departure with no guide — forcing PENDING_GUIDE.", schedule.getId());
                schedule.setStatus(TourStatus.PENDING_GUIDE);
                tourScheduleRepository.save(schedule);
            }
        }
    }
    // ══════════════════════════════════════════════════════════════════════
    // JOB 5 — Auto-Expire: change status of OPEN schedules that are past departure
    //          AND have zero bookings (all statuses) to avoid frozen records.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs every 15 minutes. Updates OPEN, guide-less schedules whose departure
     * has already passed AND that have never received any booking (including
     * cancelled/refunded ones) to EXPIRED_NO_BOOKING. These schedules are "ghost" rows — no customer
     * was ever impacted — and leaving them OPEN causes:
     * <ul>
     *   <li>Inflated "Schedules No Guide" count on the dashboard.</li>
     *   <li>Stale {@link OperationalAlert} records with negative minutesRemaining
     *       appearing in the Live Alert Queue.</li>
     * </ul>
     *
     * <p><b>Safety:</b> The booking count is re-checked inside the transaction at
     * runtime, not only at query time, so a booking created between the initial
     * query and the update will prevent expiration (race-condition safe).</p>
     *
     * <p><b>Atomicity:</b> {@code OperationalAlert} records for the schedule are
     * deleted in the same transaction before the schedule status is updated.</p>
     */
    @Scheduled(fixedRate = 900_000) // every 15 minutes
    @Transactional
    public void expireZeroBookingPastDepartureSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Candidate query: OPEN, BOOKING_CLOSED, SOLD_OUT + no guide + startDate on or before today
        List<TourSchedule> candidates = tourScheduleRepository
                .findEligibleNoGuideSchedulesOnOrBeforeDate(today);

        int purged = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();

            // Only process schedules whose departure has actually passed
            if (departure != null && now.isBefore(departure)) {
                continue;
            }

            // Re-check booking count inside the transaction (race-condition safety)
            long bookingCount = bookingRepository.countByScheduleId(schedule.getId());
            if (bookingCount > 0) {
                // A booking exists (even if cancelled/refunded) — do NOT delete.
                // The normal auto-cancel flow should handle it.
                log.info("[OPS-PURGE] Schedule #{} has {} booking(s) — skipping purge.",
                        schedule.getId(), bookingCount);
                continue;
            }

            // Safe to purge: delete associated alerts first, then the schedule
            int alertsDeleted = 0;
            try {
                operationalAlertRepository.deleteByScheduleId(schedule.getId());
                alertsDeleted = 1; // deleteByScheduleId is void; just flag success
            } catch (Exception e) {
                log.warn("[OPS-EXPIRE] Could not delete alerts for schedule #{}: {}",
                        schedule.getId(), e.getMessage());
            }

            try {
                String tourName = schedule.getTour() != null
                        ? schedule.getTour().getTourName()
                        : "Tour #" + (schedule.getTour() != null ? schedule.getTour().getId() : "?");
                schedule.setStatus(TourStatus.EXPIRED_NO_BOOKING);
                tourScheduleRepository.save(schedule);
                purged++;
                log.info("[OPS-EXPIRE] Expired ghost schedule #{} ({} — {}) with 0 bookings. Alerts also removed: {}",
                        schedule.getId(), tourName,
                        schedule.getStartDate(),
                        alertsDeleted > 0 ? "yes" : "none");
            } catch (Exception e) {
                log.error("[OPS-EXPIRE] Failed to expire schedule #{}: {}",
                        schedule.getId(), e.getMessage(), e);
            }
        }

        if (purged > 0) {
            log.info("[OPS-EXPIRE] Expired {} zero-booking past-departure ghost schedule(s).", purged);
        }
    }
}
