package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "TourID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Tour extends Base {

    @Column(name = "TourName", length = 200)
    private String tourName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Itinerary", columnDefinition = "NVARCHAR(MAX)")
    private String itinerary;

    @Column(name = "Price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Duration")
    private Integer duration;

    @Column(name = "StartLocation", length = 100)
    private String startLocation;

    @Column(name = "EndLocation", length = 100)
    private String endLocation;

    @Column(name = "Latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "Longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "TransportType", length = 50)
    private String transportType;

    @Column(name = "ChildPolicy", columnDefinition = "NVARCHAR(MAX)")
    private String childPolicy;

    @Column(name = "SuitableAges", length = 200)
    private String suitableAges;

    @Column(name = "WhyChooseUs", columnDefinition = "NVARCHAR(MAX)")
    private String whyChooseUs;

    @Column(name = "BestTime", length = 200)
    private String bestTime;

    @Column(name = "Inclusions", columnDefinition = "NVARCHAR(MAX)")
    private String inclusions;

    @Column(name = "Exclusions", columnDefinition = "NVARCHAR(MAX)")
    private String exclusions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", columnDefinition = "BIGINT")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CityID", columnDefinition = "BIGINT")
    private City city;

    @Column(name = "Rating")
    private Double rating = 0.0;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TourImage> images;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TourSchedule> schedules;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TourHighlight> highlights;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Review> reviews;

    @Column(name = "Source", length = 50)
    private String source = "LOCAL";

    @Column(name = "ExternalId", length = 100)
    private String externalId;
}
