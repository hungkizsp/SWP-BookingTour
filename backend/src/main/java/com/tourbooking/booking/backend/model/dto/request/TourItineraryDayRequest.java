package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class TourItineraryDayRequest {
    private Integer dayNumber;
    private String title;
    private String description;
    private String accommodation;
    private String meals;
    private String transportation;
    private String highlights;
    private String imageUrl;
}
