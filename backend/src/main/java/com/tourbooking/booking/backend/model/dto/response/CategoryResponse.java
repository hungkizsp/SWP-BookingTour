package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String categoryName;
    private String description;
}
