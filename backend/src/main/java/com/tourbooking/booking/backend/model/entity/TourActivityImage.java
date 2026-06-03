package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TourActivityImages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "ActivityImageID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class TourActivityImage extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", columnDefinition = "BIGINT")
    private TourSchedule schedule;

    @Column(name = "ImageURL", length = 255)
    private String imageUrl;

    @Column(name = "Caption", length = 255)
    private String caption;
}
