package com.tourbooking.booking.backend.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.mapper.TourMapper;
import com.tourbooking.booking.backend.model.dto.request.TourRequest;
import com.tourbooking.booking.backend.model.dto.response.PagedResponse;
import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.dto.response.TourResponse;
import com.tourbooking.booking.backend.model.entity.City;
import com.tourbooking.booking.backend.model.entity.Category;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.TourHighlight;
import com.tourbooking.booking.backend.model.entity.TourImage;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.repository.CategoryRepository;
import com.tourbooking.booking.backend.repository.CityRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.service.TourService;

import lombok.RequiredArgsConstructor;

@Service
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepo;
    private final CategoryRepository categoryRepo;
    private final CityRepository cityRepository;
    private final com.tourbooking.booking.backend.repository.TourScheduleRepository tourScheduleRepo;

    public TourServiceImpl(TourRepository tourRepo, CategoryRepository categoryRepo, CityRepository cityRepository, com.tourbooking.booking.backend.repository.TourScheduleRepository tourScheduleRepo) {
        this.tourRepo = tourRepo;
        this.categoryRepo = categoryRepo;
        this.cityRepository = cityRepository;
        this.tourScheduleRepo = tourScheduleRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getAllTours() {
        return tourRepo.findAll().stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourById(Long id) {
        Tour tour = tourRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        return TourMapper.toDetailResponse(tour);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> searchTours(String keyword) {
        return tourRepo.findByTourNameContainingIgnoreCase(keyword).stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getToursByCategory(Long categoryId) {
        return tourRepo.findByCategoryId(categoryId).stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TourResponse createTour(TourRequest request) {
        Tour tour = TourMapper.toEntity(request);
        
        Long categoryId = request.getCategoryId();
        if (categoryId != null) {
            Category category = categoryRepo.findById(categoryId)
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            tour.setCategory(category);
        }

        // Handle Highlights
        if (request.getHighlights() != null) {
            List<TourHighlight> highlights = request.getHighlights().stream()
                    .map(h -> {
                        TourHighlight highlight = new TourHighlight();
                        highlight.setHighlight(h);
                        highlight.setTour(tour);
                        return highlight;
                    }).toList();
            tour.setHighlights(highlights);
        }

        // Handle Images
        if (request.getImageUrls() != null) {
            List<TourImage> images = request.getImageUrls().stream()
                    .map(url -> {
                        TourImage image = new TourImage();
                        image.setImageUrl(url);
                        image.setTour(tour);
                        return image;
                    }).toList();
            tour.setImages(images);
        }

        // Handle Schedules
        if (request.getSchedules() != null) {
            List<TourSchedule> schedules = request.getSchedules().stream()
                    .map(sReq -> {
                        TourSchedule schedule = TourMapper.toScheduleEntity(sReq);
                        schedule.setTour(tour);
                        return schedule;
                    }).toList();
            tour.setSchedules(schedules);
        }

        Tour savedTour = tourRepo.save(tour);
        return TourMapper.toResponse(savedTour);
    }

    @Override
    @Transactional
    public TourResponse updateTour(Long id, TourRequest request) {
        Tour existingTour = tourRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        TourMapper.updateEntityFromRequest(existingTour, request);

        Long categoryId = request.getCategoryId();
        if (categoryId != null) {
            Category category = categoryRepo.findById(categoryId)
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            existingTour.setCategory(category);
        }

        // Update Highlights
        if (request.getHighlights() != null) {
            existingTour.getHighlights().clear();
            request.getHighlights().forEach(h -> {
                TourHighlight highlight = new TourHighlight();
                highlight.setHighlight(h);
                highlight.setTour(existingTour);
                existingTour.getHighlights().add(highlight);
            });
        }

        // Update Images
        if (request.getImageUrls() != null) {
            existingTour.getImages().clear();
            request.getImageUrls().forEach(url -> {
                TourImage image = new TourImage();
                image.setImageUrl(url);
                image.setTour(existingTour);
                existingTour.getImages().add(image);
            });
        }

        // Update Schedules
        if (request.getSchedules() != null) {
            java.util.Set<Long> incomingIds = request.getSchedules().stream()
                    .map(com.tourbooking.booking.backend.model.dto.request.TourScheduleRequest::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
                    
            for (TourSchedule existingSchedule : existingTour.getSchedules()) {
                if (!incomingIds.contains(existingSchedule.getId())) {
                    existingSchedule.setStatus(com.tourbooking.booking.backend.model.entity.enums.TourStatus.CANCELLED);
                }
            }
            
            request.getSchedules().forEach(sReq -> {
                if (sReq.getId() != null) {
                    existingTour.getSchedules().stream()
                        .filter(s -> s.getId().equals(sReq.getId()))
                        .findFirst()
                        .ifPresent(existing -> {
                            existing.setStartDate(sReq.getStartDate());
                            existing.setEndDate(sReq.getEndDate());
                            existing.setMaxSlots(sReq.getMaxSlots());
                            if (sReq.getAvailableSlots() != null) existing.setAvailableSlots(sReq.getAvailableSlots());
                            // status can be updated if sent
                            existing.setStatus(com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN);
                        });
                } else {
                    TourSchedule schedule = TourMapper.toScheduleEntity(sReq);
                    schedule.setTour(existingTour);
                    existingTour.getSchedules().add(schedule);
                }
            });
        }

        Tour updatedTour = tourRepo.save(existingTour);
        return TourMapper.toResponse(updatedTour);
    }

    @Override
    @Transactional
    public void deleteTour(Long id) {
        if (!tourRepo.existsById(id)) {
            throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        }
        tourRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> searchToursWithFilters(String keyword, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Double minRating, java.time.LocalDate startDate) {
        return tourRepo.searchToursWithFilters(keyword, minPrice, maxPrice, minRating, startDate).stream()
                .distinct()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TourResponse> browseTours(String keyword,
                                                  java.math.BigDecimal minPrice,
                                                  java.math.BigDecimal maxPrice,
                                                  Double minRating,
                                                  java.time.LocalDate startDate,
                                                  Long categoryId,
                                                  String transportType,
                                                  Long cityId,
                                                  Double lat,
                                                  Double lng,
                                                  String sortBy,
                                                  String sortDir,
                                                  Pageable pageable) {
        String normalizedSortBy = sortBy == null ? "" : sortBy.trim().toLowerCase();
        
        // Prepare keyword patterns
        String pattern = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim().toLowerCase() + "%";

        Page<Tour> page;
        if ("popularity".equals(normalizedSortBy)) {
            page = tourRepo.browseToursByPopularity(keyword, minPrice, maxPrice, minRating, startDate, categoryId, transportType, pageable);
        } else if ("distance".equals(normalizedSortBy)) {
            double[] coords = resolveCoords(cityId, lat, lng);
            page = tourRepo.browseToursByDistance(keyword, minPrice, maxPrice, minRating, startDate, categoryId, transportType, coords[0], coords[1], pageable);
        } else {
            page = tourRepo.browseTours(keyword, pattern, minPrice, maxPrice, minRating, startDate, categoryId, transportType, pageable);
        }
        return PagedResponse.<TourResponse>builder()
                .content(page.getContent().stream().map(TourMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private double[] resolveCoords(Long cityId, Double lat, Double lng) {
        if (lat != null && lng != null) {
            return new double[]{lat, lng};
        }
        if (cityId != null) {
            City city = cityRepository.findById(cityId)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
            return new double[]{city.getCenterLatitude().doubleValue(), city.getCenterLongitude().doubleValue()};
        }
        return new double[]{0.0, 0.0};
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDetailResponse> compareTours(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tourRepo.findAllById(ids).stream()
                .map(TourMapper::toDetailResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse.TourScheduleSummary getScheduleById(Long id) {
        com.tourbooking.booking.backend.model.entity.TourSchedule schedule = tourScheduleRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        
        TourDetailResponse.TourScheduleSummary s = new TourDetailResponse.TourScheduleSummary();
        s.setScheduleId(schedule.getId());
        s.setStartDate(schedule.getStartDate());
        s.setEndDate(schedule.getEndDate());
        s.setAvailableSlots(schedule.getAvailableSlots());
        s.setStatus(schedule.getStatus() == null ? null : schedule.getStatus().name());
        return s;
    }
}
