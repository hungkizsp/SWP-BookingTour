package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourFaqRepository extends JpaRepository<TourFaq, Long> {
    List<TourFaq> findByTourIsNull();
    List<TourFaq> findByTour_Id(Long tourId);
}
