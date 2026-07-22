package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.TourItineraryDayRequest;
import com.tourbooking.booking.backend.model.dto.response.TourItineraryDayResponse;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.TourItineraryDay;
import com.tourbooking.booking.backend.repository.TourItineraryDayRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.service.TourItineraryDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourItineraryDayServiceImpl implements TourItineraryDayService {

    private final TourItineraryDayRepository itineraryDayRepository;
    private final TourRepository tourRepository;

    @Override
    public List<TourItineraryDayResponse> getItineraryByTourId(Long tourId) {
        if (!tourRepository.existsById(tourId)) {
            throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        }
        return itineraryDayRepository.findByTourIdOrderByDayNumberAsc(tourId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TourItineraryDayResponse createItineraryDay(Long tourId, TourItineraryDayRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        
        TourItineraryDay day = new TourItineraryDay();
        day.setTour(tour);
        mapRequestToEntity(request, day);
        
        TourItineraryDay saved = itineraryDayRepository.save(day);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TourItineraryDayResponse updateItineraryDay(Long tourId, Long dayId, TourItineraryDayRequest request) {
        TourItineraryDay day = itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
        
        if (!day.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Itinerary day does not belong to the specified tour");
        }
        
        mapRequestToEntity(request, day);
        TourItineraryDay updated = itineraryDayRepository.save(day);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteItineraryDay(Long tourId, Long dayId) {
        TourItineraryDay day = itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
        
        if (!day.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Itinerary day does not belong to the specified tour");
        }
        
        itineraryDayRepository.delete(day);
    }

    private void mapRequestToEntity(TourItineraryDayRequest request, TourItineraryDay day) {
        day.setDayNumber(request.getDayNumber());
        day.setTitle(request.getTitle());
        day.setDescription(request.getDescription());
        day.setAccommodation(request.getAccommodation());
        day.setMeals(request.getMeals());
        day.setTransportation(request.getTransportation());
        day.setHighlights(request.getHighlights());
        day.setImageUrl(request.getImageUrl());
    }

    private TourItineraryDayResponse mapToResponse(TourItineraryDay day) {
        return TourItineraryDayResponse.builder()
                .id(day.getId())
                .tourId(day.getTour().getId())
                .dayNumber(day.getDayNumber())
                .title(day.getTitle())
                .description(day.getDescription())
                .accommodation(day.getAccommodation())
                .meals(day.getMeals())
                .transportation(day.getTransportation())
                .highlights(day.getHighlights())
                .imageUrl(day.getImageUrl())
                .createdAt(day.getCreatedAt())
                .updatedAt(day.getUpdatedAt())
                .build();
    }
}
