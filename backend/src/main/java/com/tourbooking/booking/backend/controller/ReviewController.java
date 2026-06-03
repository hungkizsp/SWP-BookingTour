package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.ReviewRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.ReviewResponse;
import com.tourbooking.booking.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved reviews (page " + page + ")")
                .data(reviewService.getAllReviewsPaged(tourId, rating, pageable))
                .build();
    }

    @GetMapping("/tour/{tourId}")
    public ApiResponse<List<ReviewResponse>> getReviewsByTour(
            @PathVariable Long tourId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved reviews for tour: " + tourId)
                .data(reviewService.getReviewsByTour(tourId, minRating, maxRating, sortBy, direction))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(@RequestBody ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Review created successfully")
                .data(reviewService.createReview(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable Long id, @RequestBody ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Review updated successfully")
                .data(reviewService.updateReview(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Review deleted successfully")
                .build();
    }
}
