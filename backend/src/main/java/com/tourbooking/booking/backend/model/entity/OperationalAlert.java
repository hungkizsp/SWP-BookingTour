package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks which alert windows (24H, 12H, 6H, 2H) have been sent for a given
 * TourSchedule so that the OperationalScheduler never sends duplicate alerts.
 * <p>
 * The unique constraint on (scheduleId, alertWindow) is the idempotency key.
 * </p>
 */
@Entity
@Table(
    name = "OperationalAlerts",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_OperationalAlerts_Schedule_Window",
        columnNames = {"ScheduleID", "AlertWindow"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationalAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;

    /** FK to TourSchedules.ScheduleID – stored as a plain Long to avoid lazy-load overhead. */
    @Column(name = "ScheduleID", nullable = false)
    private Long scheduleId;

    /**
     * One of: "24H", "12H", "6H", "2H".
     * Together with scheduleId, this forms the unique idempotency key.
     */
    @Column(name = "AlertWindow", nullable = false, length = 10)
    private String alertWindow;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
