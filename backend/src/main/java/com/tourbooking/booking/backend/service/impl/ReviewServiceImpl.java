package com.tourbooking.booking.backend.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.mapper.ReviewMapper;
import com.tourbooking.booking.backend.model.dto.request.ReviewRequest;
import com.tourbooking.booking.backend.model.dto.response.ReviewResponse;
import com.tourbooking.booking.backend.model.entity.Review;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.repository.ReviewRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final TourRepository tourRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByTour(
            Long tourId,
            Integer minRating,
            Integer maxRating,
            String sortBy,
            String direction) {

        List<Review> reviews;
        if (minRating != null && maxRating != null) {
            reviews = reviewRepo.findByTourIdAndRatingBetween(tourId, minRating, maxRating);
        } else {
            reviews = reviewRepo.findByTourId(tourId);
        }

        Comparator<Review> comparator;
        if ("rating".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(Review::getRating);
        } else {
            comparator = Comparator.comparing(Review::getCreatedAt);
        }

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return reviews.stream()
                .sorted(comparator)
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        Tour tour = tourRepo.findById(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validation: user must have a COMPLETED booking for this tour
        boolean hasCompletedBooking = tour.getSchedules().stream()
                .flatMap(schedule -> schedule.getBookings().stream())
                .anyMatch(booking -> booking.getUser().getId().equals(user.getId())
                        && booking
                                .getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.COMPLETED);

        if (!hasCompletedBooking) {
            throw new RuntimeException("USER_HAS_NO_COMPLETED_BOOKING");
        }

        if (reviewRepo.findByUser_IdAndTour_Id(user.getId(), tour.getId()).isPresent()) {
            throw new RuntimeException("REVIEW_ALREADY_EXISTS");
        }

        Review review = ReviewMapper.toEntity(request);
        review.setTour(tour);
        review.setUser(user);
        Review savedReview = reviewRepo.save(review);

        return ReviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        Review existingReview = reviewRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        ReviewMapper.updateEntityFromRequest(existingReview, request);

        if (request.getTourId() != null && !request.getTourId().equals(existingReview.getTour().getId())) {
            Tour tour = tourRepo.findById(request.getTourId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
            existingReview.setTour(tour);
        }

        if (request.getUserId() != null && !request.getUserId().equals(existingReview.getUser().getId())) {
            User user = userRepo.findById(request.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            existingReview.setUser(user);
        }

        Review updatedReview = reviewRepo.save(existingReview);
        return ReviewMapper.toResponse(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepo.findAll().stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.tourbooking.booking.backend.model.dto.response.PagedResponse<ReviewResponse> getAllReviewsPaged(
            Long tourId, Integer rating, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Review> page;

        if (tourId != null && rating != null) {
            page = reviewRepo.findByTourIdAndRating(tourId, rating, pageable);
        } else if (tourId != null) {
            page = reviewRepo.findByTourId(tourId, pageable);
        } else if (rating != null) {
            page = reviewRepo.findByRating(rating, pageable);
        } else {
            page = reviewRepo.findAll(pageable);
        }

        return com.tourbooking.booking.backend.model.dto.response.PagedResponse.<ReviewResponse>builder()
                .content(page.getContent().stream().map(ReviewMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepo.existsById(id)) {
            throw new AppException(ErrorCode.REVIEW_NOT_FOUND);
        }
        reviewRepo.deleteById(id);
    }
}
