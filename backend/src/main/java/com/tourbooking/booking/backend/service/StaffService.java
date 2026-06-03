package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.dto.response.RefundResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse;
import com.tourbooking.booking.backend.model.entity.enums.RefundStatus;
import java.util.List;

public interface StaffService {
    void confirmBooking(Long bookingId);
    void assignGuide(Long scheduleId, Long guideId);
    List<RefundResponse> listRefundRequests();
    void processRefund(Long refundId, RefundStatus status, String staffNote);
    List<UserResponse> listGuides();
    List<BookingResponse> listBookings(String status);
    List<TourScheduleResponse> listSchedules();
    TourScheduleResponse getScheduleDetails(Long id);
}
