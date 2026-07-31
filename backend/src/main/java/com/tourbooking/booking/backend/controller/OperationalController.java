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
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final com.tourbooking.booking.backend.service.TourAttendanceService tourAttendanceService;

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
        List<TourSchedule> openNoGuide = tourScheduleRepository.findEligibleSchedulesWithNoGuide(today);

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

        // ── Dedup: for each scheduleId keep only the most-urgent alert window ──
        // Priority: lowest hours = most urgent: 2H(2) > 6H(6) > 12H(12) > 24H(24)
        java.util.Map<String, Integer> windowHours = new java.util.HashMap<>();
        windowHours.put("2H",  2);
        windowHours.put("6H",  6);
        windowHours.put("12H", 12);
        windowHours.put("24H", 24);

        // Group alerts by scheduleId, keeping only the most-urgent per schedule
        java.util.Map<Long, OperationalAlert> mostUrgentBySchedule = new java.util.LinkedHashMap<>();
        for (OperationalAlert alert : allAlerts) {
            Long sid = alert.getScheduleId();
            OperationalAlert existing = mostUrgentBySchedule.get(sid);
            if (existing == null) {
                mostUrgentBySchedule.put(sid, alert);
            } else {
                int newHours = windowHours.getOrDefault(alert.getAlertWindow(), 999);
                int existingHours = windowHours.getOrDefault(existing.getAlertWindow(), 999);
                if (newHours < existingHours) {
                    mostUrgentBySchedule.put(sid, alert);
                }
            }
        }

        // Convert deduped alert records to cards (early warnings) — one per schedule
        for (OperationalAlert alert : mostUrgentBySchedule.values()) {
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

            // Skip schedules with 0 active bookings (e.g. all bookings were cancelled)
            int maxSlots = schedule.getMaxSlots() != null ? schedule.getMaxSlots() : 0;
            int availableSlots = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
            if (maxSlots - availableSlots <= 0) continue;

            // Skip stale alerts whose departure has already passed
            if (minsRemaining < 0) continue;

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

        // ── NO-SHOW Alerts (from Attendances) ──
        List<com.tourbooking.booking.backend.model.entity.TourAttendance> noShows = tourAttendanceService.getAttendancesByStatus(
                com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus.ABSENT);
        for (com.tourbooking.booking.backend.model.entity.TourAttendance att : noShows) {
            // Only show for active schedules (OPEN, IN_PROGRESS)
            if (att.getSchedule().getStatus() == TourStatus.OPEN || att.getSchedule().getStatus() == TourStatus.IN_PROGRESS) {
                Map<String, Object> card = new HashMap<>();
                card.put("type", "NO_SHOW");
                card.put("scheduleId", att.getSchedule().getId());
                card.put("tourName", att.getSchedule().getTour() != null ? att.getSchedule().getTour().getTourName() : "N/A");
                card.put("customerName", att.getBooking() != null && att.getBooking().getUser() != null ? att.getBooking().getUser().getFullName() : "N/A");
                card.put("departureDateTime", att.getSchedule().getDepartureDateTime() != null ? att.getSchedule().getDepartureDateTime().toString() : null);
                card.put("markedAt", att.getMarkedAt() != null ? att.getMarkedAt().toString() : null);
                card.put("guideName", att.getSchedule().getGuide() != null ? att.getSchedule().getGuide().getFullName() : "N/A");
                card.put("isUrgent", true); // Always urgent
                alertList.add(card);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertList);
        response.put("totalAlerts", alertList.size());
        response.put("generatedAt", now.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns aggregate operational metrics for the staff/admin dashboard.
     * All counts are now expressed in BOOKINGS (not schedules) for operational clarity.
     */
    @GetMapping("/operational-metrics")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Map<String, Object>> getOperationalMetrics() {
        // Bookings in schedules missing a guide (all relevant statuses)
        long bookingsMissingGuide = 0;
        long bookingsPendingGuide = 0;
        long bookingsRefundedByOperator = 0;
        long affectedCustomers = 0;
        java.math.BigDecimal totalRefundAmount = java.math.BigDecimal.ZERO;

        try {
            bookingsMissingGuide = bookingRepository.countValidBookingsMissingGuide();
            bookingsPendingGuide = bookingRepository.countValidBookingsPendingGuide();
            bookingsRefundedByOperator = bookingRepository.countBookingsRefundedByOperator();
            affectedCustomers = bookingRepository.countOperatorCancelledRefundedBookings(); // now counts distinct users
            java.math.BigDecimal dbRefund = bookingRepository.sumOperatorCancelledRefundAmounts();
            if (dbRefund != null) totalRefundAmount = dbRefund;
        } catch (Exception e) {
            log.warn("[OPS-METRICS] Could not compute metrics: {}", e.getMessage());
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bookingsMissingGuide", bookingsMissingGuide);
        metrics.put("bookingsPendingGuide", bookingsPendingGuide);
        metrics.put("bookingsRefundedByOperator", bookingsRefundedByOperator);
        metrics.put("affectedCustomers", affectedCustomers);
        metrics.put("totalRefundAmount", totalRefundAmount);
        metrics.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(metrics);
    }

    /**
     * URGENT Dashboard Component API
     * Returns schedules with CONFIRMED/PAID bookings, NO guide, and either past departure or within 24h.
     */
    @GetMapping("/critical-guide-alerts")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<Map<String, Object>>> getCriticalGuideAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<TourSchedule> candidates = tourScheduleRepository.findCriticalMissingGuideSchedules();

        List<Map<String, Object>> results = new ArrayList<>();
        for (TourSchedule s : candidates) {
            LocalDateTime departure = s.getDepartureDateTime();
            long minsRemaining = departure != null ? java.time.temporal.ChronoUnit.MINUTES.between(now, departure) : 0;
            
            // Filter: only past departure (minsRemaining <= 0) OR within 24h (minsRemaining <= 1440)
            if (minsRemaining > 1440) continue;

            // Calculate active bookings count and total value
            long activeBookings = 0;
            java.math.BigDecimal totalValue = java.math.BigDecimal.ZERO;
            if (s.getBookings() != null) {
                for (com.tourbooking.booking.backend.model.entity.Booking b : s.getBookings()) {
                    if (b.getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.CONFIRMED ||
                        b.getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.PAID) {
                        activeBookings++;
                        if (b.getTotalPrice() != null) {
                            totalValue = totalValue.add(b.getTotalPrice());
                        }
                    }
                }
            }

            Map<String, Object> card = new HashMap<>();
            card.put("scheduleId", s.getId());
            card.put("tourName", s.getTour() != null ? s.getTour().getTourName() : "Unknown");
            card.put("departureDateTime", departure != null ? departure.toString() : null);
            card.put("activeBookings", activeBookings);
            card.put("totalValue", totalValue);
            card.put("minutesRemaining", minsRemaining);
            card.put("status", s.getStatus().name());
            
            results.add(card);
        }

        // Sort: Past departure first (most negative first), then ascending remaining time
        results.sort((a, b) -> {
            long minA = (long) a.get("minutesRemaining");
            long minB = (long) b.get("minutesRemaining");
            return Long.compare(minA, minB);
        });

        return ResponseEntity.ok(results);
    }

    /**
     * Attendance overview for Admin/Staff — shows attendance status per schedule that is IN_PROGRESS.
     */
    @GetMapping("/attendance/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Map<String, Object>> getAttendanceForSchedule(
            @PathVariable Long scheduleId) {
        List<com.tourbooking.booking.backend.model.dto.response.AttendanceResponse> attendances =
                tourAttendanceService.getAttendancesForAdmin(scheduleId);

        TourSchedule schedule = tourScheduleRepository.findById(scheduleId).orElse(null);

        long present = attendances.stream().filter(a -> a.getStatus() ==
                com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus.PRESENT).count();
        long absent = attendances.stream().filter(a -> a.getStatus() ==
                com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus.ABSENT).count();
        long pending = attendances.stream().filter(a -> a.getStatus() ==
                com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus.PENDING).count();

        Map<String, Object> result = new HashMap<>();
        result.put("scheduleId", scheduleId);
        result.put("tourName", schedule != null && schedule.getTour() != null ? schedule.getTour().getTourName() : "N/A");
        result.put("currentProgress", schedule != null ? schedule.getCurrentProgress() : null);
        result.put("presentCount", present);
        result.put("absentCount", absent);
        result.put("pendingCount", pending);
        result.put("attendances", attendances);
        return ResponseEntity.ok(result);
    }
}
