package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.TourRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.PagedResponse;
import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.dto.response.TourResponse;
import com.tourbooking.booking.backend.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @GetMapping
    public ApiResponse<List<TourResponse>> getAllTours() {
        return ApiResponse.<List<TourResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved all tours")
                .data(tourService.getAllTours())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TourDetailResponse> getTourById(@PathVariable Long id) {
        return ApiResponse.<TourDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved tour details")
                .data(tourService.getTourById(id))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<List<TourResponse>> searchTours(@RequestParam String keyword) {
        return ApiResponse.<List<TourResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Search results for: " + keyword)
                .data(tourService.searchTours(keyword))
                .build();
    }

    @GetMapping("/filter")
    public ApiResponse<List<TourResponse>> filterTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) java.time.LocalDate startDate) {
        return ApiResponse.<List<TourResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Filtered tours")
                .data(tourService.searchToursWithFilters(keyword, minPrice, maxPrice, minRating, startDate))
                .build();
    }

    // UC01 + UC02 + UC03 + UC04: browse tours with filters + sorting + paging
    @GetMapping("/browse")
    public ApiResponse<PagedResponse<TourResponse>> browseTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String transportType,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        String normalizedSortBy = sortBy == null ? "" : sortBy.trim().toLowerCase();

        // For computed sorts (popularity/distance), sorting happens in SQL
        Pageable pageable;
        if (Arrays.asList("popularity", "distance").contains(normalizedSortBy)) {
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        } else {
            Sort sort = Sort.by("price");
            if (Arrays.asList("price", "rating", "tourname", "createdat").contains(normalizedSortBy)) {
                String property = switch (normalizedSortBy) {
                    case "tourname" -> "tourName";
                    case "createdat" -> "createdAt";
                    default -> normalizedSortBy;
                };
                sort = Sort.by(property);
            }
            sort = "desc".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
        }

        return ApiResponse.<PagedResponse<TourResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Browse tours")
                .data(tourService.browseTours(keyword, minPrice, maxPrice, minRating, startDate, categoryId, transportType, cityId, lat, lng, sortBy, sortDir, pageable))
                .build();
    }

    // UC06: compare multiple tours
    @GetMapping("/compare")
    public ApiResponse<List<TourDetailResponse>> compareTours(@RequestParam String ids) {
        List<Long> tourIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .toList();

        return ApiResponse.<List<TourDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Compare tours")
                .data(tourService.compareTours(tourIds))
                .build();
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<TourResponse>> getToursByCategory(@PathVariable Long categoryId) {
        return ApiResponse.<List<TourResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Tours in category: " + categoryId)
                .data(tourService.getToursByCategory(categoryId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TourResponse> createTour(@RequestBody TourRequest request) {
        return ApiResponse.<TourResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tour created successfully")
                .data(tourService.createTour(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<TourResponse> updateTour(@PathVariable Long id, @RequestBody TourRequest request) {
        return ApiResponse.<TourResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tour updated successfully")
                .data(tourService.updateTour(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Tour deleted successfully")
                .build();
    }

    @GetMapping("/schedules/{id}")
    public ApiResponse<TourDetailResponse.TourScheduleSummary> getScheduleById(@PathVariable Long id) {
        return ApiResponse.<TourDetailResponse.TourScheduleSummary>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved schedule details")
                .data(tourService.getScheduleById(id))
                .build();
    }
}
