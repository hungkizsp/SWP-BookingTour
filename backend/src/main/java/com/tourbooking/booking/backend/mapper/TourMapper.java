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
            response.setSchedules(tour.getSchedules().stream().map(TourMapper::toScheduleSummary).toList());
        }

        response.setItinerary(tour.getItinerary());
        response.setSuitableAges(tour.getSuitableAges());
        response.setChildPolicy(tour.getChildPolicy());
        response.setWhyChooseUs(tour.getWhyChooseUs());
        response.setExternalId(tour.getExternalId());

        return response;
    }

    private static TourDetailResponse.TourScheduleSummary toScheduleSummary(TourSchedule schedule) {
        TourDetailResponse.TourScheduleSummary s = new TourDetailResponse.TourScheduleSummary();
        s.setScheduleId(schedule.getId());
        s.setStartDate(schedule.getStartDate());
        s.setEndDate(schedule.getEndDate());
        s.setAvailableSlots(schedule.getAvailableSlots());
        s.setMaxSlots(schedule.getMaxSlots());
        s.setStatus(schedule.getStatus() == null ? null : schedule.getStatus().name());
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
        schedule.setMaxSlots(request.getMaxSlots());
        // AvailableSlots should be MaxSlots initially
        schedule.setAvailableSlots(request.getMaxSlots());
        schedule.setStatus(TourStatus.OPEN);
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
