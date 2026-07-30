package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.TourCompareRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.TourCompareResponse;
import com.tourbooking.booking.backend.service.TourCompareService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
public class TourCompareController {

    private final TourCompareService tourCompareService;

    @PostMapping("/compare-ai")
    public ApiResponse<TourCompareResponse> compareToursAi(@Valid @RequestBody TourCompareRequest request) {
        TourCompareResponse response = tourCompareService.compareTours(request);
        return ApiResponse.<TourCompareResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tour comparison completed successfully")
                .data(response)
                .build();
    }
}
