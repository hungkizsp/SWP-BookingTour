package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.BookingRequest;
import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.entity.Booking;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        if (booking == null)
            return null;
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        if (booking.getUser() != null) {
            response.setUserId(booking.getUser().getId());
            response.setUserFullName(booking.getUser().getFullName());
        }
        if (booking.getSchedule() != null) {
            response.setScheduleId(booking.getSchedule().getId());
            if (booking.getSchedule().getTour() != null) {
                response.setTourId(booking.getSchedule().getTour().getId());
                response.setTourName(booking.getSchedule().getTour().getTourName());
            }
        }
        response.setBookingDate(booking.getBookingDate());
        response.setNumberOfPeople(booking.getNumberOfPeople());
        response.setTotalPrice(booking.getTotalPrice());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setDiscountCode(booking.getDiscountCode());
        response.setStatus(booking.getStatus());
        return response;
    }

    public static Booking toEntity(BookingRequest request) {
        if (request == null)
            return null;
        Booking booking = new Booking();
        updateEntityFromRequest(booking, request);
        return booking;
    }

    public static void updateEntityFromRequest(Booking booking, BookingRequest request) {
        if (request == null || booking == null)
            return;
        if (request.getNumberOfPeople() != null)
            booking.setNumberOfPeople(request.getNumberOfPeople());
        if (request == null || booking == null)
            return;
        if (request.getNumberOfPeople() != null)
            booking.setNumberOfPeople(request.getNumberOfPeople());
        if (request.getTotalPrice() != null)
            booking.setTotalPrice(request.getTotalPrice());
        if (request.getDiscountCode() != null)
            booking.setDiscountCode(request.getDiscountCode());
        if (request.getStatus() != null)
            booking.setStatus(request.getStatus());
    }
}
