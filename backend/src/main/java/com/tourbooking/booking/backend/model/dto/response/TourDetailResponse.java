package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

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

    @Data
    public static class TourScheduleSummary {
        private Long scheduleId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer availableSlots;
        private Integer maxSlots;
        private String status;
    }
}
