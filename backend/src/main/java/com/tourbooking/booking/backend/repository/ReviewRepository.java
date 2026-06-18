package com.tourbooking.booking.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tourbooking.booking.backend.model.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Fetch all reviews for a given tour by traversing booking -> schedule -> tour.
     * Used by the public tour-detail page.
     */
    @Query("SELECT r FROM Review r " +
           "JOIN r.booking b " +
           "JOIN b.schedule s " +
           "WHERE s.tour.id = :tourId")
    List<Review> findByTourId(@Param("tourId") Long tourId);

    @Query("SELECT r FROM Review r " +
           "JOIN r.booking b " +
           "JOIN b.schedule s " +
           "WHERE s.tour.id = :tourId AND r.rating BETWEEN :minRating AND :maxRating")
    List<Review> findByTourIdAndRatingBetween(@Param("tourId") Long tourId,
                                              @Param("minRating") Integer minRating,
                                              @Param("maxRating") Integer maxRating);

    @Query("SELECT r FROM Review r " +
           "JOIN r.booking b " +
           "JOIN b.schedule s " +
           "WHERE s.tour.id = :tourId")
    org.springframework.data.domain.Page<Review> findByTourId(
            @Param("tourId") Long tourId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Review> findByRating(
            Integer rating,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM Review r " +
           "JOIN r.booking b " +
           "JOIN b.schedule s " +
           "WHERE s.tour.id = :tourId AND r.rating = :rating")
    org.springframework.data.domain.Page<Review> findByTourIdAndRating(
            @Param("tourId") Long tourId,
            @Param("rating") Integer rating,
            org.springframework.data.domain.Pageable pageable);

    /** Core uniqueness check: one review per booking. */
    Optional<Review> findByBookingId(Long bookingId);

}
