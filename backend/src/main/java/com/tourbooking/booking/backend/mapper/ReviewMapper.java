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

        if (review.getBooking() != null) {
            response.setBookingId(review.getBooking().getId());
            // Resolve tour from booking -> schedule -> tour for backward-compat fields
            if (review.getBooking().getSchedule() != null
                    && review.getBooking().getSchedule().getTour() != null) {
                response.setTourId(review.getBooking().getSchedule().getTour().getId());
                response.setTourName(review.getBooking().getSchedule().getTour().getTourName());
            }
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
        // Booking and User must be set in the Service layer
    }
}
