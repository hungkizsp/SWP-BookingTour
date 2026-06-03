package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "AvailableSlots")
    private Integer availableSlots;

    @Column(name = "MaxSlots")
    private Integer maxSlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private TourStatus status = TourStatus.OPEN;

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
}
