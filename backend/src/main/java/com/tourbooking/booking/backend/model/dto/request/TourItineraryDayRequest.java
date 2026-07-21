package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class TourItineraryDayRequest {
    @jakarta.validation.constraints.NotNull(message = "Ngày lịch trình không được để trống")
    @jakarta.validation.constraints.Positive(message = "Ngày lịch trình phải là số nguyên dương")
    private Integer dayNumber;
    
    @jakarta.validation.constraints.NotBlank(message = "Tiêu đề ngày lịch trình không được để trống")
    private String title;
    
    @jakarta.validation.constraints.NotBlank(message = "Mô tả ngày lịch trình không được để trống")
    private String description;
    private String accommodation;
    private String meals;
    private String transportation;
    private String highlights;
    private String imageUrl;
}
