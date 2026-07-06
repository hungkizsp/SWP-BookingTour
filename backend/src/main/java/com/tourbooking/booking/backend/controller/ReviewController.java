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

    /** Paged listing — supports optional tourId / rating filters (admin/public). */
    @GetMapping
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "id"));
        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved reviews (page " + page + ")")
                .data(reviewService.getAllReviewsPaged(tourId, rating, pageable))
                .build();
    }

    /** Public tour-detail page: all reviews for a given tour, sortable/filterable. */
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

    /**
     * Create a new review.
     * Payload: { "bookingId": 42, "rating": 5, "comment": "..." }
     * The authenticated user is resolved server-side; no userId in body.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(@RequestBody ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Review saved successfully")
                .data(reviewService.createReview(request))
                .build();
    }

    /** Update rating/comment of an existing review (owner only). */
    @PutMapping("/{id}")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable Long id, @RequestBody ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Review updated successfully")
                .data(reviewService.updateReview(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Review deleted successfully")
                .build();
    }
}
