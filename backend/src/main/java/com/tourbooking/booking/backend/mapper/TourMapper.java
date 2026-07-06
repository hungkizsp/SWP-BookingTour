package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.TourRequest;
import com.tourbooking.booking.backend.model.dto.request.TourScheduleRequest;
import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.dto.response.TourResponse;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.TourImage;
import com.tourbooking.booking.backend.model.entity.TourHighlight;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;

import java.util.stream.Collectors;

public class TourMapper {

    public static TourResponse toResponse(Tour tour) {
        if (tour == null)
            return null;
        TourResponse response = new TourResponse();
        response.setId(tour.getId());
        response.setTourName(tour.getTourName());
        response.setDescription(tour.getDescription());
        response.setPrice(tour.getPrice());
        response.setDuration(tour.getDuration());
        response.setStartLocation(tour.getStartLocation());
        response.setEndLocation(tour.getEndLocation());
        response.setRating(tour.getRating());
        response.setTransportType(tour.getTransportType());

        if (tour.getImages() != null && !tour.getImages().isEmpty()) {
            response.setImageUrls(tour.getImages().stream().map(TourImage::getImageUrl).collect(Collectors.toList()));
            response.setImageUrl(tour.getImages().get(0).getImageUrl());
        }
        response.setExternalId(tour.getExternalId());

        if (tour.getCategory() != null) {
            response.setCategoryId(tour.getCategory().getId());
            response.setCategoryName(tour.getCategory().getCategoryName());
        }

        return response;
    }

    public static TourDetailResponse toDetailResponse(Tour tour) {
        if (tour == null)
            return null;
        TourDetailResponse response = new TourDetailResponse();
        response.setId(tour.getId());
        response.setTourName(tour.getTourName());
        response.setDescription(tour.getDescription());
        response.setPrice(tour.getPrice());
        response.setDuration(tour.getDuration());
        response.setStartLocation(tour.getStartLocation());
        response.setEndLocation(tour.getEndLocation());
        response.setRating(tour.getRating());
        response.setTransportType(tour.getTransportType());

        if (tour.getCategory() != null) {
            response.setCategoryName(tour.getCategory().getCategoryName());
        }

        if (tour.getImages() != null && !tour.getImages().isEmpty()) {
            response.setImageUrls(tour.getImages().stream().map(TourImage::getImageUrl).collect(Collectors.toList()));
            response.setImageUrl(tour.getImages().get(0).getImageUrl());
        }

        if (tour.getHighlights() != null) {
            response.setHighlights(
                    tour.getHighlights().stream().map(TourHighlight::getHighlight).collect(Collectors.toList()));
        }

        if (tour.getSchedules() != null) {
            response.setSchedules(tour.getSchedules().stream()
                    .filter(s -> s.getStatus() == null
                            || (s.getStatus() != TourStatus.CANCELLED
                                && s.getStatus() != TourStatus.COMPLETED))
                    .map(TourMapper::toScheduleSummary).toList());
        }

        response.setItinerary(tour.getItinerary());
        response.setSuitableAges(tour.getSuitableAges());
        response.setChildPolicy(tour.getChildPolicy());
        response.setWhyChooseUs(tour.getWhyChooseUs());
        response.setExternalId(tour.getExternalId());

        // Comparison metrics — reviewCount set by Service via ReviewRepository
        response.setReviewCount(0); // default; overridden in Service
        response.setItineraryDaysCount(tour.getItineraryDays() != null ? tour.getItineraryDays().size() : 0);
        if (response.getSchedules() != null && !response.getSchedules().isEmpty()) {
            TourDetailResponse.TourScheduleSummary closest = null;
            for (TourDetailResponse.TourScheduleSummary s : response.getSchedules()) {
                if (!Boolean.TRUE.equals(s.getIsExpired())
                        && (closest == null || s.getStartDate().isBefore(closest.getStartDate()))) {
                    closest = s;
                }
            }
            response.setClosestScheduleSlots(closest != null ? closest.getAvailableSlots() : 0);
        } else {
            response.setClosestScheduleSlots(0);
        }

        return response;
    }

    public static TourDetailResponse.TourScheduleSummary toScheduleSummary(TourSchedule schedule) {
        TourDetailResponse.TourScheduleSummary s = new TourDetailResponse.TourScheduleSummary();
        s.setScheduleId(schedule.getId());
        s.setStartDate(schedule.getStartDate());
        s.setEndDate(schedule.getEndDate());
        s.setDepartureTime(schedule.getDepartureTime());
        s.setReturnTime(schedule.getReturnTime());
        s.setAvailableSlots(schedule.getAvailableSlots());
        s.setMaxSlots(schedule.getMaxSlots());
        s.setStatus(schedule.getStatus() == null ? null : schedule.getStatus().name());
        s.setBookingDeadline(schedule.getEffectiveBookingDeadline());

        boolean isExpired = false;
        if (schedule.getStartDate() != null) {
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            if (schedule.getStartDate().isBefore(today)) {
                isExpired = true;
            } else if (schedule.getStartDate().isEqual(today)) {
                if (schedule.getDepartureTime() != null) {
                    java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
                    if (!schedule.getDepartureTime().isAfter(now)) {
                        isExpired = true;
                    }
                }
            }
        }
        s.setIsExpired(isExpired);

        return s;
    }

    public static Tour toEntity(TourRequest request) {
        if (request == null)
            return null;
        Tour tour = new Tour();
        updateEntityFromRequest(tour, request);
        return tour;
    }

    public static TourSchedule toScheduleEntity(TourScheduleRequest request) {
        if (request == null)
            return null;
        TourSchedule schedule = new TourSchedule();
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setDepartureTime(request.getDepartureTime());
        schedule.setReturnTime(request.getReturnTime());
        schedule.setMaxSlots(request.getMaxSlots());
        // AvailableSlots should be MaxSlots initially
        schedule.setAvailableSlots(request.getMaxSlots());
        schedule.setStatus(TourStatus.OPEN);
        // If an explicit deadline is provided, use it; otherwise the entity helper will fall back to departure datetime
        if (request.getBookingDeadline() != null) {
            schedule.setBookingDeadline(request.getBookingDeadline());
        }
        return schedule;
    }

    public static void updateEntityFromRequest(Tour tour, TourRequest request) {
        if (request == null || tour == null)
            return;
        if (request.getTourName() != null)
            tour.setTourName(request.getTourName());
        if (request.getDescription() != null)
            tour.setDescription(request.getDescription());
        if (request.getPrice() != null)
            tour.setPrice(request.getPrice());
        if (request.getDuration() != null)
            tour.setDuration(request.getDuration());
        if (request.getItinerary() != null)
            tour.setItinerary(request.getItinerary());
        if (request.getStartLocation() != null)
            tour.setStartLocation(request.getStartLocation());
        if (request.getEndLocation() != null)
            tour.setEndLocation(request.getEndLocation());
        if (request.getTransportType() != null)
            tour.setTransportType(request.getTransportType());
        if (request.getExternalId() != null)
            tour.setExternalId(request.getExternalId());
        // Category and other collections should be handled in the Service layer
    }
}
