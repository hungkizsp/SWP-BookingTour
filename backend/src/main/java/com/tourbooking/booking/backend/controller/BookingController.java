package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.BookingRequest;
import com.tourbooking.booking.backend.model.dto.request.RefundRequest;
import com.tourbooking.booking.backend.model.dto.request.VoucherRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.dto.response.VoucherResponse;
import com.tourbooking.booking.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Booking Management", description = "APIs for managing tour bookings")
public class BookingController {

    private final BookingService bookingService;
    private final com.tourbooking.booking.backend.repository.UserRepository userRepository;

    public BookingController(BookingService bookingService,
                           com.tourbooking.booking.backend.repository.UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    /**
     * UC18: Get booking history for authenticated customer with filters and search
     */
    @GetMapping("/history")
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.BookingHistoryResponse> getBookingHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo,
            @RequestParam(required = false) java.math.BigDecimal priceMin,
            @RequestParam(required = false) java.math.BigDecimal priceMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
            return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.BookingHistoryResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized - Please login to view booking history")
                    .build();
        }

        String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        com.tourbooking.booking.backend.model.entity.User currentUser =
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new com.tourbooking.booking.backend.exception.AppException(
                                com.tourbooking.booking.backend.exception.ErrorCode.USER_NOT_FOUND));

        com.tourbooking.booking.backend.model.dto.response.BookingHistoryResponse response =
                bookingService.getBookingHistory(currentUser.getId(), search, status, dateFrom, dateTo, priceMin, priceMax, page, size);

        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.BookingHistoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved booking history")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<?> getAllBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return ApiResponse.<org.springframework.data.domain.Page<BookingResponse>>builder()
                    .code(HttpStatus.OK.value())
                    .message("Successfully retrieved paginated bookings")
                    .data(bookingService.getAllBookingsPaginated(page, size))
                    .build();
        }
        return ApiResponse.<List<BookingResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved all bookings")
                .data(bookingService.getAllBookings())
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<BookingResponse>> getBookingsByUserId(@PathVariable Long userId) {
        return ApiResponse.<List<BookingResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved bookings for user: " + userId)
                .data(bookingService.getBookingsByUserId(userId))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved booking details")
                .data(bookingService.getBookingById(id))
                .build();
    }

    /**
     * UC19: Get detailed booking information
     */
    @GetMapping("/{id}/detail")
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.BookingDetailResponse> getBookingDetail(@PathVariable Long id) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
            return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.BookingDetailResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized")
                    .build();
        }

        String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        com.tourbooking.booking.backend.model.entity.User currentUser =
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new com.tourbooking.booking.backend.exception.AppException(
                                com.tourbooking.booking.backend.exception.ErrorCode.USER_NOT_FOUND));

        com.tourbooking.booking.backend.model.dto.response.BookingDetailResponse detail =
                bookingService.getBookingDetail(id, currentUser.getId());

        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.BookingDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved booking detail")
                .data(detail)
                .build();
    }

    @PostMapping
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Booking created successfully")
                .data(bookingService.createBooking(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<BookingResponse> updateBooking(
            @PathVariable Long id,
            @RequestBody BookingRequest request
    ) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Booking updated successfully")
                .data(bookingService.updateBooking(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Booking deleted successfully")
                .build();
    }

    // UC15
    @PostMapping("/apply-voucher")
    public ApiResponse<VoucherResponse> applyVoucher(@RequestBody VoucherRequest request) {
        return ApiResponse.<VoucherResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Voucher processing complete")
                .data(bookingService.applyVoucher(request))
                .build();
    }

    // UC20
    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Booking cancelled successfully")
                .data(bookingService.cancelBooking(id))
                .build();
    }

    // UC21
    @PostMapping("/{id}/refund")
    public ApiResponse<BookingResponse> requestRefund(
            @PathVariable Long id,
            @RequestBody RefundRequest request
    ) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Refund requested successfully")
                .data(bookingService.requestRefund(id, request))
                .build();
    }

    // UC22
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        com.tourbooking.booking.backend.model.entity.User currentUser =
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new com.tourbooking.booking.backend.exception.AppException(
                                com.tourbooking.booking.backend.exception.ErrorCode.USER_NOT_FOUND));

        byte[] pdfBytes = bookingService.generateInvoice(id, currentUser.getId());

        return ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-BK-" + id + ".pdf"
                )
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdfBytes);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Booking status updated to " + status)
                .data(bookingService.updateBookingStatus(id, status))
                .build();
    }

    // Reschedule
    @PostMapping("/reschedule")
    public ApiResponse<BookingResponse> rescheduleBooking(
            @Valid @RequestBody com.tourbooking.booking.backend.model.dto.request.RescheduleRequest request) {
        return ApiResponse.<BookingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Đổi ngày khởi hành thành công")
                .data(bookingService.rescheduleBooking(request))
                .build();
    }

    /**
     * GET /api/v1/bookings/{bookingId}/reschedule-candidates
     * Returns future-only schedules for the same tour, filtered by occupiedSlots.
     * The frontend calls this so it gets a server-authoritative list rather than
     * filtering the entire tour payload on the client.
     */
    @GetMapping("/{bookingId}/reschedule-candidates")
    public ApiResponse<?> getRescheduleCandidates(@PathVariable Long bookingId) {
        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("Reschedule candidates retrieved")
                .data(bookingService.getRescheduleCandidates(bookingId))
                .build();
    }
}
