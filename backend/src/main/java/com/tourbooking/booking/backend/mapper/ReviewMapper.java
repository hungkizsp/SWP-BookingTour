package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.ReviewRequest;
import com.tourbooking.booking.backend.model.dto.response.ReviewResponse;
import com.tourbooking.booking.backend.model.entity.Review;

public class ReviewMapper {

    public static ReviewResponse toResponse(Review review) {
        if (review == null)
            return null;
        ReviewResponse response = new ReviewResponse();
        response.setReviewId(review.getId());
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
            response.setUserName(review.getUser().getFullName());
        }
        if (review.getTour() != null) {
            response.setTourId(review.getTour().getId());
            response.setTourName(review.getTour().getTourName());
        }
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    public static Review toEntity(ReviewRequest request) {
        if (request == null)
            return null;
        Review review = new Review();
        updateEntityFromRequest(review, request);
        return review;
    }

    public static void updateEntityFromRequest(Review review, ReviewRequest request) {
        if (request == null || review == null)
            return;
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        // Tour and User should be set in Service layer if needed
    }
}
