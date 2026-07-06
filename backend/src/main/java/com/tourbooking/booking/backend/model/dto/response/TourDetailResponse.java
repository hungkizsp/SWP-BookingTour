package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TourDetailResponse {
    private Long id;
    private String tourName;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private String itinerary;
    private String startLocation;
    private String endLocation;
    private Double rating;
    private String transportType;
    private String categoryName;
    private String suitableAges;
    private String childPolicy;
    private String whyChooseUs;
    private List<String> imageUrls;
    private String imageUrl;
    private String externalId;
    private List<String> highlights;
    private List<TourScheduleSummary> schedules;
    
    // New metrics for Comparison Engine
    private Integer reviewCount;
    private Integer itineraryDaysCount;
    private Integer closestScheduleSlots;
    private BigDecimal pricePerDay;
    private String meals;
    private String accommodation;
    private Integer maxGroupSize;

    @Data
    public static class TourScheduleSummary {
        private Long scheduleId;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalTime departureTime;
        private LocalTime returnTime;
        private Integer availableSlots;
        private Integer maxSlots;
        private String status;
        /** ISO-8601 datetime string for the booking deadline. */
        private LocalDateTime bookingDeadline;
        private Boolean isExpired;
    }
}
