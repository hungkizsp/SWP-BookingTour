package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CityResponse {
    private Long id;
    private String cityName;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
}
