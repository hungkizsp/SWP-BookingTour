package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.RefundResponse;
import com.tourbooking.booking.backend.model.entity.enums.RefundStatus;
import com.tourbooking.booking.backend.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<com.tourbooking.booking.backend.model.dto.response.BookingResponse>> listBookings(@RequestParam(required = false) String status) {
        return ApiResponse.<List<com.tourbooking.booking.backend.model.dto.response.BookingResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Bookings retrieved successfully")
                .data(staffService.listBookings(status))
                .build();
    }

    @PatchMapping("/bookings/{id}/confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<String> confirmBooking(@PathVariable Long id) {
        staffService.confirmBooking(id);
        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Booking confirmed successfully")
                .data(null)
                .build();
    }

    @GetMapping("/guides")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<com.tourbooking.booking.backend.model.dto.response.UserResponse>> listGuides() {
        return ApiResponse.<List<com.tourbooking.booking.backend.model.dto.response.UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Guides retrieved successfully")
                .data(staffService.listGuides())
                .build();
    }

    @PatchMapping("/schedules/{id}/assign-guide")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<String> assignGuide(@PathVariable Long id, @RequestParam Long guideId) {
        staffService.assignGuide(id, guideId);
        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Guide assigned successfully")
                .data(null)
                .build();
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse>> listSchedules() {
        return ApiResponse.<List<com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Schedules retrieved successfully")
                .data(staffService.listSchedules())
                .build();
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse> getScheduleDetails(@PathVariable Long id) {
        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Schedule details retrieved successfully")
                .data(staffService.getScheduleDetails(id))
                .build();
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<RefundResponse>> listRefundRequests() {
        return ApiResponse.<List<RefundResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Refund requests retrieved successfully")
                .data(staffService.listRefundRequests())
                .build();
    }

    @PatchMapping("/refunds/{id}/process")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<String> processRefund(
            @PathVariable Long id,
            @RequestParam RefundStatus status,
            @RequestParam(required = false) String staffNote) {
        staffService.processRefund(id, status, staffNote);
        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Refund request processed successfully")
                .data(null)
                .build();
    }
}
