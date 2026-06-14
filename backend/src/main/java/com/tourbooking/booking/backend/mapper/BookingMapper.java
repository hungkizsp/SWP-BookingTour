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
        if (booking.getId() != null) {
            java.time.LocalDateTime dateToUse = booking.getBookingDate() != null ? booking.getBookingDate()
                    : java.time.LocalDateTime.now();
            response.setBookingCode(String.format("TOUR-%d-%06d", dateToUse.getYear(), booking.getId()));
        }
        if (booking.getUser() != null) {
            response.setUserId(booking.getUser().getId());
            response.setUserFullName(booking.getUser().getFullName());
            response.setUserEmail(booking.getUser().getEmail());
        }
        if (booking.getSchedule() != null) {
            response.setScheduleId(booking.getSchedule().getId());
            response.setDepartureDate(booking.getSchedule().getStartDate());
            response.setReturnDate(booking.getSchedule().getEndDate());
            if (booking.getSchedule().getTour() != null) {
                response.setTourId(booking.getSchedule().getTour().getId());
                response.setTourName(booking.getSchedule().getTour().getTourName());
                response.setTourItinerary(booking.getSchedule().getTour().getItinerary());
            }
            if (booking.getSchedule().getGuide() != null) {
                response.setGuideFullName(booking.getSchedule().getGuide().getFullName());
                response.setGuidePhone(booking.getSchedule().getGuide().getPhoneNumber());
                response.setGuideAvatar(booking.getSchedule().getGuide().getAvatarUrl());
                response.setGuideBio(booking.getSchedule().getGuide().getBio());
                response.setGuideExperience(booking.getSchedule().getGuide().getExperienceYears());
                response.setGuideGender(booking.getSchedule().getGuide().getGender());
                response.setGuideDateOfBirth(booking.getSchedule().getGuide().getDateOfBirth());
            }
        }
        response.setBookingDate(booking.getBookingDate());
        response.setNumberOfPeople(booking.getNumberOfPeople());
        response.setTotalPrice(booking.getTotalPrice());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setDiscountCode(booking.getDiscountCode());
        response.setStatus(booking.getStatus());

        if (booking.getPassengers() != null) {
            java.util.List<com.tourbooking.booking.backend.model.dto.response.PassengerResponse> passengerResponses = booking
                    .getPassengers().stream().map(p -> {
                        com.tourbooking.booking.backend.model.dto.response.PassengerResponse pr = new com.tourbooking.booking.backend.model.dto.response.PassengerResponse();
                        pr.setId(p.getId());
                        pr.setFullName(p.getFullName());
                        pr.setDateOfBirth(p.getDateOfBirth());
                        pr.setIdNumber(p.getIdNumber());
                        pr.setPassengerType(p.getPassengerType());
                        return pr;
                    }).collect(java.util.stream.Collectors.toList());
            response.setPassengers(passengerResponses);
        }

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
        if (request.getAdultCount() != null) {
            booking.setNumberOfPeople(request.getNumberOfPeople());
            booking.setOccupiedSlots(request.getOccupiedSlots());
        }
        if (request.getTotalPrice() != null)
            booking.setTotalPrice(request.getTotalPrice());
        if (request.getDiscountCode() != null)
            booking.setDiscountCode(request.getDiscountCode());
        if (request.getStatus() != null)
            booking.setStatus(request.getStatus());
    }
}
