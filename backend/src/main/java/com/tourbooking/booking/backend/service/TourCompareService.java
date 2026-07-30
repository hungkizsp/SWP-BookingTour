package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.TourCompareRequest;
import com.tourbooking.booking.backend.model.dto.response.TourCompareResponse;

public interface TourCompareService {
    TourCompareResponse compareTours(TourCompareRequest request);
}
