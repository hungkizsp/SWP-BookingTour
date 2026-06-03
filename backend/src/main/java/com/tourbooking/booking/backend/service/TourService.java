package com.tourbooking.booking.backend.service;

import java.util.List;

import com.tourbooking.booking.backend.model.dto.request.TourRequest;
import com.tourbooking.booking.backend.model.dto.response.PagedResponse;
import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.dto.response.TourResponse;
import org.springframework.data.domain.Pageable;

public interface TourService {
    List<TourResponse> getAllTours();

    TourDetailResponse getTourById(Long id);

    List<TourResponse> searchTours(String keyword);

    List<TourResponse> getToursByCategory(Long categoryId);

    TourResponse createTour(TourRequest request);

    TourResponse updateTour(Long id, TourRequest request);

    void deleteTour(Long id);

    List<TourResponse> searchToursWithFilters(String keyword, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Double minRating, java.time.LocalDate startDate);

    PagedResponse<TourResponse> browseTours(String keyword,
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
                                           Pageable pageable);

    List<TourDetailResponse> compareTours(List<Long> ids);
    
    TourDetailResponse.TourScheduleSummary getScheduleById(Long id);
}
