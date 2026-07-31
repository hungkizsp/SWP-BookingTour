package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TourAttendances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", nullable = false)
    private TourSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus status;

    @Column(name = "MarkedAt")
    private LocalDateTime markedAt;

    @Column(name = "LateNote", length = 500)
    private String lateNote;

    @Column(name = "LateMinutes")
    private Integer lateMinutes;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
