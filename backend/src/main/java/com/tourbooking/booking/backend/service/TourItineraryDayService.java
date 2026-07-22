package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.TourItineraryDayRequest;
import com.tourbooking.booking.backend.model.dto.response.TourItineraryDayResponse;

import java.util.List;

public interface TourItineraryDayService {
    List<TourItineraryDayResponse> getItineraryByTourId(Long tourId);
    TourItineraryDayResponse createItineraryDay(Long tourId, TourItineraryDayRequest request);
    TourItineraryDayResponse updateItineraryDay(Long tourId, Long dayId, TourItineraryDayRequest request);
    void deleteItineraryDay(Long tourId, Long dayId);
}
