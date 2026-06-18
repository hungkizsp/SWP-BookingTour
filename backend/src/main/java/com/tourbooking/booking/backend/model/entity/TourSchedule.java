package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "TourSchedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "ScheduleID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class TourSchedule extends Base {

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourID", columnDefinition = "BIGINT")
    private Tour tour;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    /** Departure time on start date (e.g. 07:00). */
    @Column(name = "DepartureTime")
    private LocalTime departureTime;

    /** Return time on end date (e.g. 18:00). */
    @Column(name = "ReturnTime")
    private LocalTime returnTime;

    @Column(name = "AvailableSlots")
    private Integer availableSlots;

    @Column(name = "MaxSlots")
    private Integer maxSlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private TourStatus status = TourStatus.OPEN;

    /**
     * Deadline for accepting bookings.
     * After this timestamp, new bookings are rejected and status transitions to BOOKING_CLOSED.
     * Defaults to departure datetime if not explicitly set.
     */
    @Column(name = "BookingDeadline")
    private LocalDateTime bookingDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GuideID", columnDefinition = "BIGINT")
    private User guide;

    @Column(name = "CurrentProgress", columnDefinition = "NVARCHAR(MAX)")
    private String currentProgress;

    @Column(name = "ReportContent", columnDefinition = "NVARCHAR(MAX)")
    private String reportContent;

    @Column(name = "ReportSubmittedAt")
    private LocalDateTime reportSubmittedAt;

    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TourActivityImage> activityImages;

    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Booking> bookings;

    // ── Convenience helpers ──────────────────────────────────────────────

    /**
     * Computes the full departure datetime from startDate + departureTime.
     * Falls back to start-of-day if departureTime is null.
     */
    public LocalDateTime getDepartureDateTime() {
        if (startDate == null) return null;
        return departureTime != null
                ? LocalDateTime.of(startDate, departureTime)
                : startDate.atStartOfDay();
    }

    /**
     * Computes the full return datetime from endDate + returnTime.
     * Falls back to end-of-day (23:59) if returnTime is null.
     */
    public LocalDateTime getReturnDateTime() {
        if (endDate == null) return null;
        return returnTime != null
                ? LocalDateTime.of(endDate, returnTime)
                : endDate.atTime(23, 59);
    }

    /**
     * Returns the effective booking deadline.
     * If no explicit deadline was set, defaults to departure datetime.
     */
    public LocalDateTime getEffectiveBookingDeadline() {
        if (bookingDeadline != null) return bookingDeadline;
        return getDepartureDateTime();
    }
}
