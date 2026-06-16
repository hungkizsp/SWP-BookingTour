package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.BookingRequest;
import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.dto.response.FinancialReportResponse;
import java.math.BigDecimal;
import java.util.List;

public interface BookingService {
    List<BookingResponse> getAllBookings();
    org.springframework.data.domain.Page<BookingResponse> getAllBookingsPaginated(int page, int size);
    List<BookingResponse> getBookingsByUserId(Long userId);
    BookingResponse getBookingById(Long id);
    BookingResponse createBooking(BookingRequest request);
    BookingResponse updateBooking(Long id, BookingRequest request);
    void deleteBooking(Long id);

    List<FinancialReportResponse> getFinancialReport(String start, String end, String type, String status, boolean includeTest);

    long countActiveBookings();

    BigDecimal getMonthlyRevenue();
    void generateTestData();

    // UC15
    com.tourbooking.booking.backend.model.dto.response.VoucherResponse applyVoucher(com.tourbooking.booking.backend.model.dto.request.VoucherRequest request);
    
    // UC20
    BookingResponse cancelBooking(Long id);
    com.tourbooking.booking.backend.model.dto.response.CancelBookingResponse cancelBookingWithReason(
            Long bookingId, Long customerId, String reason, String additionalDetails);
    
    // UC21
    BookingResponse requestRefund(Long id, com.tourbooking.booking.backend.model.dto.request.RefundRequest request);
    
    // UC22
    byte[] downloadInvoice(Long id);

    // Admin action
    BookingResponse updateBookingStatus(Long id, String status);
}
