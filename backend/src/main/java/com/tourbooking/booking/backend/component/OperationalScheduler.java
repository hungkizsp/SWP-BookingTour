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

    // ─── Alert window definitions ────────────────────────────────────────────
    private static final long[] ALERT_HOURS = {24L, 12L, 6L, 2L};
    private static final String[] ALERT_WINDOWS = {"24H", "12H", "6H", "2H"};

    private final TourScheduleRepository tourScheduleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final OperationalAlertRepository operationalAlertRepository;
    private final MailService mailService;

    // ══════════════════════════════════════════════════════════════════════
    // JOB 1 — Early-Warning Alerts (24H / 12H / 6H / 2H before departure)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs every 3 minutes. Scans upcoming OPEN schedules without a guide and fires
     * one-time notifications for each threshold window that has been entered.
     */
    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void sendEarlyWarningAlerts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<TourSchedule> candidates = tourScheduleRepository.findOpenSchedulesWithNoGuide(today);

        int alertsSent = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null) continue;

            // Bypass workflows if schedule has 0 bookings
            int maxSlots = schedule.getMaxSlots() != null ? schedule.getMaxSlots() : 0;
            int availableSlots = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
            if (maxSlots - availableSlots <= 0) {
                continue;
            }

            for (int i = 0; i < ALERT_HOURS.length; i++) {
                long hoursAhead = ALERT_HOURS[i];
                String window = ALERT_WINDOWS[i];

                // Check: are we inside (or past) this window?
                LocalDateTime windowStart = departure.minusHours(hoursAhead);
                if (now.isBefore(windowStart)) {
                    continue; // Not yet in this window — skip
                }

                // Idempotency: has this alert already been sent?
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
                    // Note: MailService.sendGuideAssignedEmail repurposed pattern — using sendMonthlyReportEmail for admin alerts
                    mailService.sendOperationalAlert(subject, body);
                } catch (Exception e) {
                    log.warn("[OPS-ALERT] Could not send alert email for schedule #{}: {}", schedule.getId(), e.getMessage());
                }

                alertsSent++;
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

        List<TourSchedule> candidates = tourScheduleRepository.findOpenNoGuideSchedulesOnOrBeforeDate(today);

        int transitioned = 0;
        for (TourSchedule schedule : candidates) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null) continue;

            // Bypass workflows if schedule has 0 bookings
            int maxSlots = schedule.getMaxSlots() != null ? schedule.getMaxSlots() : 0;
            int availableSlots = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
            if (maxSlots - availableSlots <= 0) {
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

            // Step B: Refund all CONFIRMED bookings
            List<Booking> confirmedBookings = bookingRepository.findConfirmedByScheduleId(schedule.getId());
            for (Booking booking : confirmedBookings) {
                processOperatorCancellationRefund(booking, schedule);
            }

            log.info("[OPS-CANCEL] Schedule #{} cancelled. {} booking(s) refunded.", schedule.getId(), confirmedBookings.size());
        }
    }

    /**
     * Processes a full refund for a single booking due to CANCELLED_BY_OPERATOR.
     */
    private void processOperatorCancellationRefund(Booking booking, TourSchedule schedule) {
        try {
            // 1. Set booking to REFUNDED
            booking.setStatus(BookingStatus.REFUNDED);
            bookingRepository.save(booking);

            // 2. Find the latest successful payment and mark it REFUNDED
            Payment payment = paymentRepository
                    .findFirstByBooking_IdAndStatusOrderByPaymentDateDesc(booking.getId(), PaymentStatus.SUCCESS)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                // 3. Save refund log
                PaymentLog refundLog = new PaymentLog();
                refundLog.setPayment(payment);
                refundLog.setLogMessage("OPERATOR_CANCELLATION_REFUND | scheduleId=" + schedule.getId() +
                        " | reason=GUIDE_NOT_ASSIGNED | amount=" + payment.getAmount() +
                        " | refundedAt=" + LocalDateTime.now());
                paymentLogRepository.save(refundLog);
            }

            // 4. Send customer notification email
            if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                try {
                    mailService.sendOperatorCancellationRefundEmail(
                            booking.getUser().getEmail(),
                            booking.getUser().getFullName(),
                            booking.getId(),
                            booking.getTotalPrice());
                } catch (Exception emailEx) {
                    log.warn("[OPS-CANCEL] Failed to send refund email for booking #{}: {}",
                            booking.getId(), emailEx.getMessage());
                }
            }

            log.info("[OPS-CANCEL] Booking #{} refunded (GUIDE_NOT_ASSIGNED).", booking.getId());
        } catch (Exception ex) {
            log.error("[OPS-CANCEL] Error processing refund for booking #{}: {}", booking.getId(), ex.getMessage(), ex);
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
                .findOpenNoGuideSchedulesOnOrBeforeDate(today);

        for (TourSchedule schedule : candidatesForProgress) {
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure == null || now.isBefore(departure)) {
                continue; // not yet at departure
            }

            // Bypass workflows if schedule has 0 bookings
            int maxSlots = schedule.getMaxSlots() != null ? schedule.getMaxSlots() : 0;
            int availableSlots = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
            if (maxSlots - availableSlots <= 0) {
                continue;
            }
            // This schedule has reached departure with no guide — block IN_PROGRESS
            if (schedule.getStatus() == TourStatus.OPEN) {
                log.warn("[OPS-GUARD] Schedule #{} reached departure with no guide — forcing PENDING_GUIDE.", schedule.getId());
                schedule.setStatus(TourStatus.PENDING_GUIDE);
                tourScheduleRepository.save(schedule);
            }
        }
    }
}
