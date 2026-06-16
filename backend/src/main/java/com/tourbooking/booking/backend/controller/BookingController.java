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
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
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
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Request refund",
        description = "Request a refund for a cancelled booking with bank account information"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Refund request created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Not cancelled, duplicate request, or missing bank info"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Refund already exists")
    })
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.RefundRequestResponse> requestRefund(
            @PathVariable Long id,
            @RequestBody @jakarta.validation.Valid com.tourbooking.booking.backend.model.dto.request.RefundRequest request) {
        
        // Extract authenticated customer ID
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
            return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.RefundRequestResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized")
                    .build();
        }
        
        String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        com.tourbooking.booking.backend.model.entity.User currentUser = 
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new com.tourbooking.booking.backend.exception.AppException(
                                com.tourbooking.booking.backend.exception.ErrorCode.USER_NOT_FOUND));
        
        com.tourbooking.booking.backend.model.dto.response.RefundRequestResponse response = 
                bookingService.requestRefundEnhanced(id, currentUser.getId(), request);
        
        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.RefundRequestResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Refund request submitted successfully")
                .data(response)
                .build();
    }

    // UC22
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        byte[] pdfBytes = bookingService.downloadInvoice(id);
        return ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice_" + id + ".pdf"
                )
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
}
