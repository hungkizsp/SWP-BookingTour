package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

import java.util.List;

@Data
public class TourRequest {
    private String tourName;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private String itinerary;
    private String startLocation;
    private String endLocation;
    private String transportType;
    private Long categoryId;
    private List<String> imageUrls;
    private List<String> highlights;
    private List<TourScheduleRequest> schedules;
    private String externalId;
}
