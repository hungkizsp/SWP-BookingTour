package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.OperationalAlert;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.OperationalAlertRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes REST endpoints for the Operational Readiness Dashboard.
 *
 * <ul>
 *   <li>GET /api/v1/admin/operational-alerts   — list of unresolved alerts</li>
 *   <li>GET /api/v1/admin/operational-metrics  — aggregate counts & refund totals</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class OperationalController {

    private final OperationalAlertRepository operationalAlertRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final BookingRepository bookingRepository;

    /**
     * Returns all active (unresolved) operational alerts — OPEN schedules that have had
     * alerts fired but still have no guide assigned, plus any PENDING_GUIDE schedules.
     * Each alert card is enriched with departure time and minutes remaining.
     */
    @GetMapping("/operational-alerts")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Map<String, Object>> getOperationalAlerts() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Gather all alerts from the DB
        List<OperationalAlert> allAlerts = operationalAlertRepository.findAll();

        // Gather PENDING_GUIDE schedules (not yet in alerts list but urgent)
        List<TourSchedule> pendingGuideSchedules = tourScheduleRepository
                .findByStatusIn(List.of(TourStatus.PENDING_GUIDE));

        // Gather OPEN schedules with no guide (upcoming, for alert context)
        List<TourSchedule> openNoGuide = tourScheduleRepository.findOpenSchedulesWithNoGuide(today);

        List<Map<String, Object>> alertList = new ArrayList<>();

        // Convert PENDING_GUIDE schedules to alert cards (most urgent)
        for (TourSchedule s : pendingGuideSchedules) {
            LocalDateTime departure = s.getDepartureDateTime();
            long minsRemaining = departure != null
                    ? java.time.temporal.ChronoUnit.MINUTES.between(now, departure)
                    : 0;

            Map<String, Object> card = new HashMap<>();
            card.put("type", "PENDING_GUIDE");
            card.put("scheduleId", s.getId());
            card.put("tourName", s.getTour() != null ? s.getTour().getTourName() : "Schedule #" + s.getId());
            card.put("departureDateTime", departure != null ? departure.toString() : null);
            card.put("minutesRemaining", minsRemaining);
            card.put("status", s.getStatus().name());
            card.put("isUrgent", true);
            alertList.add(card);
        }

        // Convert alert records to cards (early warnings)
        for (OperationalAlert alert : allAlerts) {
            // Check if the associated schedule still has no guide (still relevant)
            TourSchedule schedule = null;
            try {
                schedule = tourScheduleRepository.findById(alert.getScheduleId()).orElse(null);
            } catch (Exception ignored) {}

            if (schedule == null) continue;

            // Skip if guide is now assigned (alert resolved)
            if (schedule.getGuide() != null) continue;

            // Skip if already PENDING_GUIDE (already shown above)
            if (schedule.getStatus() == TourStatus.PENDING_GUIDE) continue;

            // Skip cancelled/completed schedules
            if (schedule.getStatus() == TourStatus.CANCELLED ||
                schedule.getStatus() == TourStatus.CANCELLED_BY_OPERATOR ||
                schedule.getStatus() == TourStatus.COMPLETED) continue;

            LocalDateTime departure = schedule.getDepartureDateTime();
            long minsRemaining = departure != null
                    ? java.time.temporal.ChronoUnit.MINUTES.between(now, departure)
                    : 0;

            Map<String, Object> card = new HashMap<>();
            card.put("type", "EARLY_WARNING");
            card.put("alertWindow", alert.getAlertWindow());
            card.put("scheduleId", alert.getScheduleId());
            card.put("tourName", schedule.getTour() != null ? schedule.getTour().getTourName() : "Schedule #" + schedule.getId());
            card.put("departureDateTime", departure != null ? departure.toString() : null);
            card.put("minutesRemaining", minsRemaining);
            card.put("alertCreatedAt", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : null);
            card.put("status", schedule.getStatus().name());
            card.put("isUrgent", minsRemaining < 120); // < 2 hours remaining = urgent
            alertList.add(card);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertList);
        response.put("totalAlerts", alertList.size());
        response.put("generatedAt", now.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns aggregate operational metrics for the admin dashboard:
     * - Upcoming OPEN schedules missing a guide (warning zone)
     * - Schedules currently in PENDING_GUIDE (critical zone)
     * - Schedules in CANCELLED_BY_OPERATOR (failed due to no guide)
     * - Total affected customers and refund totals
     */
    @GetMapping("/operational-metrics")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Map<String, Object>> getOperationalMetrics() {
        LocalDate today = LocalDate.now();

        long openNoGuideCount = tourScheduleRepository.findOpenSchedulesWithNoGuide(today).size();
        long pendingGuideCount = tourScheduleRepository.countByStatus(TourStatus.PENDING_GUIDE);
        long cancelledByOperatorCount = tourScheduleRepository.countByStatus(TourStatus.CANCELLED_BY_OPERATOR);

        long affectedCustomers = 0;
        java.math.BigDecimal totalRefundAmount = java.math.BigDecimal.ZERO;
        try {
            affectedCustomers = bookingRepository.countOperatorCancelledRefundedBookings();
            java.math.BigDecimal dbRefund = bookingRepository.sumOperatorCancelledRefundAmounts();
            if (dbRefund != null) totalRefundAmount = dbRefund;
        } catch (Exception e) {
            log.warn("[OPS-METRICS] Could not compute refund totals: {}", e.getMessage());
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("openSchedulesWithNoGuide", openNoGuideCount);
        metrics.put("pendingGuideSchedules", pendingGuideCount);
        metrics.put("cancelledByOperatorSchedules", cancelledByOperatorCount);
        metrics.put("affectedCustomers", affectedCustomers);
        metrics.put("totalRefundAmount", totalRefundAmount);
        metrics.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(metrics);
    }
}
