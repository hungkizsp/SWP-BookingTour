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
import com.tourbooking.booking.backend.repository.ReviewRepository;
import com.tourbooking.booking.backend.repository.TourImageRepository;
import com.tourbooking.booking.backend.repository.TourItineraryDayRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.service.TourService;

@Service
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepo;
    private final CategoryRepository categoryRepo;
    private final CityRepository cityRepository;
    private final com.tourbooking.booking.backend.repository.TourScheduleRepository tourScheduleRepo;
    private final ReviewRepository reviewRepo;
    private final TourItineraryDayRepository itineraryDayRepo;
    private final TourImageRepository tourImageRepo;

    public TourServiceImpl(TourRepository tourRepo, CategoryRepository categoryRepo, CityRepository cityRepository,
            com.tourbooking.booking.backend.repository.TourScheduleRepository tourScheduleRepo,
            ReviewRepository reviewRepo,
            TourItineraryDayRepository itineraryDayRepo,
            TourImageRepository tourImageRepo) {
        this.tourRepo = tourRepo;
        this.categoryRepo = categoryRepo;
        this.cityRepository = cityRepository;
        this.tourScheduleRepo = tourScheduleRepo;
        this.reviewRepo = reviewRepo;
        this.itineraryDayRepo = itineraryDayRepo;
        this.tourImageRepo = tourImageRepo;
    }

    /**
     * Post-loads images for a list of tours (needed because native queries and JPQL
     * without JOIN FETCH do not hydrate the lazy images collection).
     */
    private void enrichToursWithImages(java.util.List<Tour> tours) {
        if (tours == null || tours.isEmpty()) return;
        java.util.List<Long> ids = tours.stream().map(Tour::getId).toList();
        java.util.Map<Long, java.util.List<com.tourbooking.booking.backend.model.entity.TourImage>> byTourId =
            tourImageRepo.findByTourIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(img -> img.getTour().getId()));
        for (Tour t : tours) {
            t.setImages(byTourId.getOrDefault(t.getId(), java.util.List.of()));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getAllTours() {
        return tourRepo.findAllWithBasicDetails().stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourById(Long id) {
        // findByIdWithDetails uses @EntityGraph to eagerly load images, highlights, schedules etc.
        Tour tour = tourRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isAdmin) {
            boolean hasFutureSchedules = tour.getSchedules().stream()
                    .anyMatch(s -> s.getStartDate() != null && s.getStartDate().isAfter(java.time.LocalDate.now().minusDays(1)));
            boolean allSuspended = hasFutureSchedules && tour.getSchedules().stream()
                    .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(java.time.LocalDate.now().minusDays(1)))
                    .allMatch(s -> s.getStatus() == com.tourbooking.booking.backend.model.entity.enums.TourStatus.SUSPENDED);
            
            if (allSuspended) {
                throw new AppException(ErrorCode.TOUR_NOT_FOUND);
            }
        }

        TourDetailResponse resp = TourMapper.toDetailResponse(tour);
        resp.setReviewCount((int) reviewRepo.findByTourId(id).size());
        resp.setItineraryDaysCount((int) itineraryDayRepo.findByTourIdOrderByDayNumberAsc(id).size());
        return resp;
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
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalTime now = java.time.LocalTime.now();
            List<TourSchedule> schedules = request.getSchedules().stream()
                    .map(sReq -> {
                        if (sReq.getStartDate() != null && sReq.getStartDate().equals(today) && 
                            sReq.getDepartureTime() != null && sReq.getDepartureTime().isBefore(now)) {
                            throw new IllegalArgumentException("Giờ khởi hành cho ngày hôm nay phải lớn hơn giờ hiện tại!");
                        }
                        TourSchedule schedule = TourMapper.toScheduleEntity(sReq);
                        schedule.setTour(tour);
                        return schedule;
                    }).toList();
            tour.setSchedules(schedules);
        }

        // Handle Itinerary
        if (request.getItineraryDays() != null) {
            java.util.Set<Integer> dayNumbers = new java.util.HashSet<>();
            for (com.tourbooking.booking.backend.model.dto.request.TourItineraryDayRequest dayReq : request.getItineraryDays()) {
                if (!dayNumbers.add(dayReq.getDayNumber())) {
                    throw new IllegalArgumentException("Ngày lịch trình bị trùng lặp: " + dayReq.getDayNumber());
                }
            }
            List<com.tourbooking.booking.backend.model.entity.TourItineraryDay> itineraryDays = request.getItineraryDays().stream()
                    .map(dayReq -> {
                        com.tourbooking.booking.backend.model.entity.TourItineraryDay day = new com.tourbooking.booking.backend.model.entity.TourItineraryDay();
                        day.setDayNumber(dayReq.getDayNumber());
                        day.setTitle(dayReq.getTitle());
                        day.setDescription(dayReq.getDescription());
                        day.setAccommodation(dayReq.getAccommodation());
                        day.setMeals(dayReq.getMeals());
                        day.setTransportation(dayReq.getTransportation());
                        day.setHighlights(dayReq.getHighlights());
                        day.setImageUrl(dayReq.getImageUrl());
                        day.setTour(tour);
                        return day;
                    }).toList();
            tour.setItineraryDays(itineraryDays);
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

            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalTime now = java.time.LocalTime.now();

            request.getSchedules().forEach(sReq -> {
                if (sReq.getStartDate() != null && sReq.getStartDate().equals(today) && 
                    sReq.getDepartureTime() != null && sReq.getDepartureTime().isBefore(now)) {
                    throw new IllegalArgumentException("Giờ khởi hành cho ngày hôm nay phải lớn hơn giờ hiện tại!");
                }

                if (sReq.getId() != null) {
                    existingTour.getSchedules().stream()
                            .filter(s -> s.getId().equals(sReq.getId()))
                            .findFirst()
                            .ifPresent(existing -> {
                                existing.setStartDate(sReq.getStartDate());
                                existing.setEndDate(sReq.getEndDate());
                                if (sReq.getDepartureTime() != null)
                                    existing.setDepartureTime(sReq.getDepartureTime());
                                if (sReq.getReturnTime() != null)
                                    existing.setReturnTime(sReq.getReturnTime());
                                if (sReq.getBookingDeadline() != null)
                                    existing.setBookingDeadline(sReq.getBookingDeadline());
                                existing.setMaxSlots(sReq.getMaxSlots());
                                if (sReq.getAvailableSlots() != null)
                                    existing.setAvailableSlots(sReq.getAvailableSlots());
                                existing.setStatus(com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN);
                            });
                } else {
                    TourSchedule schedule = TourMapper.toScheduleEntity(sReq);
                    schedule.setTour(existingTour);
                    existingTour.getSchedules().add(schedule);
                }
            });
        }

        // Handle Itinerary
        if (request.getItineraryDays() != null) {
            java.util.Set<Integer> dayNumbers = new java.util.HashSet<>();
            for (com.tourbooking.booking.backend.model.dto.request.TourItineraryDayRequest dayReq : request.getItineraryDays()) {
                if (!dayNumbers.add(dayReq.getDayNumber())) {
                    throw new IllegalArgumentException("Ngày lịch trình bị trùng lặp: " + dayReq.getDayNumber());
                }
            }
            
            if (existingTour.getItineraryDays() != null) {
                existingTour.getItineraryDays().clear();
            }
            
            java.util.List<com.tourbooking.booking.backend.model.entity.TourItineraryDay> oldDays = itineraryDayRepo.findByTourIdOrderByDayNumberAsc(existingTour.getId());
            if (!oldDays.isEmpty()) {
                itineraryDayRepo.deleteAll(oldDays);
                itineraryDayRepo.flush(); // Force DELETE statements to execute before new INSERTs
            }
            
            List<com.tourbooking.booking.backend.model.entity.TourItineraryDay> newItineraryDays = request.getItineraryDays().stream()
                    .map(dayReq -> {
                        com.tourbooking.booking.backend.model.entity.TourItineraryDay day = new com.tourbooking.booking.backend.model.entity.TourItineraryDay();
                        day.setDayNumber(dayReq.getDayNumber());
                        day.setTitle(dayReq.getTitle());
                        day.setDescription(dayReq.getDescription());
                        day.setAccommodation(dayReq.getAccommodation());
                        day.setMeals(dayReq.getMeals());
                        day.setTransportation(dayReq.getTransportation());
                        day.setHighlights(dayReq.getHighlights());
                        day.setImageUrl(dayReq.getImageUrl());
                        day.setTour(existingTour);
                        return day;
                    }).toList();
                    
            if (existingTour.getItineraryDays() != null) {
                existingTour.getItineraryDays().addAll(newItineraryDays);
            } else {
                existingTour.setItineraryDays(newItineraryDays);
            }
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
    public List<TourResponse> searchToursWithFilters(String keyword, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, Double minRating, java.time.LocalDate startDate) {
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
        String pattern = (keyword == null || keyword.trim().isEmpty()) ? null
                : "%" + keyword.trim().toLowerCase() + "%";

        // Admin/Staff can see all tours including suspended ones
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        boolean hideSuspended = !isAdminOrStaff;

        Page<Tour> page;
        if ("popularity".equals(normalizedSortBy)) {
            page = tourRepo.browseToursByPopularity(keyword, minPrice, maxPrice, minRating, startDate, categoryId,
                    transportType, hideSuspended ? 1 : 0, pageable);
        } else if ("distance".equals(normalizedSortBy)) {
            double[] coords = resolveCoords(cityId, lat, lng);
            page = tourRepo.browseToursByDistance(keyword, minPrice, maxPrice, minRating, startDate, categoryId,
                    transportType, coords[0], coords[1], hideSuspended ? 1 : 0, pageable);
        } else {
            page = tourRepo.browseTours(keyword, pattern, minPrice, maxPrice, minRating, startDate, categoryId,
                    transportType, hideSuspended, pageable);
        }

        // Post-load images: native queries & JPQL without JOIN FETCH do not hydrate
        // the lazy images collection. We batch-load in a single extra query.
        java.util.List<Tour> tours = page.getContent();
        enrichToursWithImages(tours);

        return PagedResponse.<TourResponse>builder()
                .content(tours.stream().map(TourMapper::toResponse).toList())
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
            return new double[] { lat, lng };
        }
        if (cityId != null) {
            City city = cityRepository.findById(cityId)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
            return new double[] { city.getCenterLatitude().doubleValue(), city.getCenterLongitude().doubleValue() };
        }
        return new double[] { 0.0, 0.0 };
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDetailResponse> compareTours(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tourRepo.findAllById(ids).stream()
                .map(tour -> {
                    TourDetailResponse resp = TourMapper.toDetailResponse(tour);
                    resp.setReviewCount(reviewRepo.findByTourId(tour.getId()).size());
                    
                    java.util.List<com.tourbooking.booking.backend.model.entity.TourItineraryDay> days = itineraryDayRepo.findByTourIdOrderByDayNumberAsc(tour.getId());
                    resp.setItineraryDaysCount(days.size());
                    
                    if (tour.getPrice() != null && tour.getDuration() != null && tour.getDuration() > 0) {
                        resp.setPricePerDay(tour.getPrice().divide(java.math.BigDecimal.valueOf(tour.getDuration()), java.math.RoundingMode.HALF_UP));
                    }
                    
                    if (!days.isEmpty()) {
                        java.util.Set<String> mealSet = new java.util.HashSet<>();
                        java.util.Set<String> accSet = new java.util.HashSet<>();
                        for (com.tourbooking.booking.backend.model.entity.TourItineraryDay day : days) {
                            if (day.getMeals() != null && !day.getMeals().isBlank()) mealSet.add(day.getMeals().trim());
                            if (day.getAccommodation() != null && !day.getAccommodation().isBlank()) accSet.add(day.getAccommodation().trim());
                        }
                        resp.setMeals(mealSet.isEmpty() ? "Tự túc" : String.join(", ", mealSet));
                        resp.setAccommodation(accSet.isEmpty() ? "Không có" : String.join(", ", accSet));
                    } else {
                        resp.setMeals("Đang cập nhật");
                        resp.setAccommodation("Đang cập nhật");
                    }
                    
                    int maxGroup = 0;
                    if (tour.getSchedules() != null) {
                        for (com.tourbooking.booking.backend.model.entity.TourSchedule ts : tour.getSchedules()) {
                            if (ts.getMaxSlots() != null && ts.getMaxSlots() > maxGroup) {
                                maxGroup = ts.getMaxSlots();
                            }
                        }
                    }
                    resp.setMaxGroupSize(maxGroup > 0 ? maxGroup : 30);
                    
                    // Map itinerary days
                    java.util.List<com.tourbooking.booking.backend.model.dto.response.TourItineraryDayResponse> dayResponses = days.stream()
                        .map(d -> com.tourbooking.booking.backend.model.dto.response.TourItineraryDayResponse.builder()
                            .id(d.getId())
                            .tourId(tour.getId())
                            .dayNumber(d.getDayNumber())
                            .title(d.getTitle())
                            .description(d.getDescription())
                            .accommodation(d.getAccommodation())
                            .meals(d.getMeals())
                            .transportation(d.getTransportation())
                            .highlights(d.getHighlights())
                            .imageUrl(d.getImageUrl())
                            .build())
                        .toList();
                    resp.setItineraryDayList(dayResponses);
                    
                    // Map destinations
                    java.util.List<String> dests = new java.util.ArrayList<>();
                    if (tour.getStartLocation() != null && !tour.getStartLocation().isBlank()) dests.add(tour.getStartLocation().trim());
                    if (tour.getEndLocation() != null && !tour.getEndLocation().isBlank() && !dests.contains(tour.getEndLocation().trim())) dests.add(tour.getEndLocation().trim());
                    if (tour.getCity() != null && tour.getCity().getCityName() != null && !tour.getCity().getCityName().isBlank() && !dests.contains(tour.getCity().getCityName().trim())) {
                        dests.add(tour.getCity().getCityName().trim());
                    }
                    resp.setDestinations(dests);
                    
                    return resp;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse.TourScheduleSummary getScheduleById(Long id) {
        com.tourbooking.booking.backend.model.entity.TourSchedule schedule = tourScheduleRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

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
        return s;
    }
}
