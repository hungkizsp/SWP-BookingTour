package com.tourbooking.booking.backend.service.impl;

import java.util.Comparator;
import java.util.List;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.mapper.ReviewMapper;
import com.tourbooking.booking.backend.model.dto.request.ReviewRequest;
import com.tourbooking.booking.backend.model.dto.response.ReviewResponse;
import com.tourbooking.booking.backend.model.entity.Review;
import com.tourbooking.booking.backend.repository.ReviewRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: resolve the currently authenticated User from SecurityContext
    // ──────────────────────────────────────────────────────────────────────────
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public read methods (unchanged behaviour, repo queries updated)
    // ──────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE — core refactored method
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {

        // 1. Fetch the target Booking
        Booking booking = bookingRepo.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // 2. Ownership check — must be the booking's own customer
        User currentUser = resolveCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 3. Status check — booking must be COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.TOUR_NOT_COMPLETED_YET);
        }

        // 4. Uniqueness check — one review per booking
        if (reviewRepo.findByBookingId(booking.getId()).isPresent()) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 5. Build and persist the review
        Review review = ReviewMapper.toEntity(request);
        review.setBooking(booking);
        review.setUser(currentUser);
        Review savedReview = reviewRepo.save(review);

        return ReviewMapper.toResponse(savedReview);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UPDATE — only rating/comment can be changed; booking/user are immutable
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        Review existingReview = reviewRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Ownership check before allowing an edit
        User currentUser = resolveCurrentUser();
        if (!existingReview.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        ReviewMapper.updateEntityFromRequest(existingReview, request);

        Review updatedReview = reviewRepo.save(existingReview);
        return ReviewMapper.toResponse(updatedReview);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ — admin/staff listing helpers
    // ──────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepo.existsById(id)) {
            throw new AppException(ErrorCode.REVIEW_NOT_FOUND);
        }
        reviewRepo.deleteById(id);
    }
}
