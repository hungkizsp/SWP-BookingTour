package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TourResponse {
    private Long id;
    private String tourName;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private String startLocation;
    private String endLocation;
    private Double rating;
    private String transportType;
    private List<String> imageUrls;
    private String imageUrl;
    private String externalId;
}
