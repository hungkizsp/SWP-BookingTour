package com.tourbooking.booking.backend.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {
    private String location;
    private String forecast;
    private Double tempCelsius;
    private String advice;
}
