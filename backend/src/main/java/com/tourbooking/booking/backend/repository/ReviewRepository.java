package com.tourbooking.booking.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tourbooking.booking.backend.model.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTourId(Long tourId);

    List<Review> findByTourIdAndRatingBetween(Long tourId, Integer minRating, Integer maxRating);

    org.springframework.data.domain.Page<Review> findByTourId(Long tourId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Review> findByRating(Integer rating, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Review> findByTourIdAndRating(Long tourId, Integer rating, org.springframework.data.domain.Pageable pageable);

}
