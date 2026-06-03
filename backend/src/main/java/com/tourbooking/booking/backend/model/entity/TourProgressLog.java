package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TourProgressLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "LogID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class TourProgressLog extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", columnDefinition = "BIGINT")
    private TourSchedule schedule;

    @Column(name = "Content", columnDefinition = "NVARCHAR(MAX)")
    private String content;
}
