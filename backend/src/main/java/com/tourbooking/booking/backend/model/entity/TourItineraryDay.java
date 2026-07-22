package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tour_itinerary_day", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tour_id", "day_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourItineraryDay extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "accommodation", length = 255)
    private String accommodation;

    @Column(name = "meals", length = 100)
    private String meals;

    @Column(name = "transportation", length = 100)
    private String transportation;

    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
