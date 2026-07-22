package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourItineraryDayRepository extends JpaRepository<TourItineraryDay, Long> {
    List<TourItineraryDay> findByTourIdOrderByDayNumberAsc(Long tourId);
}
