package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TourItineraryDayResponse {
    private Long id;
    private Long tourId;
    private Integer dayNumber;
    private String title;
    private String description;
    private String accommodation;
    private String meals;
    private String transportation;
    private String highlights;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
