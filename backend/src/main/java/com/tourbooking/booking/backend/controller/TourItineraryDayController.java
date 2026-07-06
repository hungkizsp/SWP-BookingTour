package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.TourItineraryDayRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.TourItineraryDayResponse;
import com.tourbooking.booking.backend.service.TourItineraryDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TourItineraryDayController {

    private final TourItineraryDayService itineraryService;

    @GetMapping("/tours/{tourId}/itinerary")
    public ApiResponse<List<TourItineraryDayResponse>> getItinerary(@PathVariable Long tourId) {
        return ApiResponse.<List<TourItineraryDayResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy lịch trình thành công")
                .data(itineraryService.getItineraryByTourId(tourId))
                .build();
    }

    @PostMapping("/admin/tours/{tourId}/itinerary")
    public ApiResponse<TourItineraryDayResponse> createItineraryDay(
            @PathVariable Long tourId,
            @RequestBody TourItineraryDayRequest request) {
        return ApiResponse.<TourItineraryDayResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tạo ngày lịch trình thành công")
                .data(itineraryService.createItineraryDay(tourId, request))
                .build();
    }

    @PutMapping("/admin/tours/{tourId}/itinerary/{dayId}")
    public ApiResponse<TourItineraryDayResponse> updateItineraryDay(
            @PathVariable Long tourId,
            @PathVariable Long dayId,
            @RequestBody TourItineraryDayRequest request) {
        return ApiResponse.<TourItineraryDayResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật ngày lịch trình thành công")
                .data(itineraryService.updateItineraryDay(tourId, dayId, request))
                .build();
    }

    @DeleteMapping("/admin/tours/{tourId}/itinerary/{dayId}")
    public ApiResponse<Void> deleteItineraryDay(
            @PathVariable Long tourId,
            @PathVariable Long dayId) {
        itineraryService.deleteItineraryDay(tourId, dayId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa ngày lịch trình thành công")
                .build();
    }
}
