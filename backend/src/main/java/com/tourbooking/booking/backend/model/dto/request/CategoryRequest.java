package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class CategoryRequest {
    private String categoryName;
    private String description;
}
