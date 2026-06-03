package com.tourbooking.booking.backend.service;

import java.util.List;

import com.tourbooking.booking.backend.model.dto.request.ReviewRequest;
import com.tourbooking.booking.backend.model.dto.response.ReviewResponse;

public interface ReviewService {

    List<ReviewResponse> getReviewsByTour(
            Long tourId,
            Integer minRating,
            Integer maxRating,
            String sortBy,
            String direction);

    List<ReviewResponse> getAllReviews();
    com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse> getAllReviewsPaged(Long tourId, Integer rating, org.springframework.data.domain.Pageable pageable);

    ReviewResponse createReview(ReviewRequest request);

    ReviewResponse updateReview(Long id, ReviewRequest request);

    void deleteReview(Long id);

}
