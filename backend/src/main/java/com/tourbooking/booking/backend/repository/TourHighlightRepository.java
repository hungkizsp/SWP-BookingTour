package com.tourbooking.booking.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.TourHighlight;

@Repository
public interface TourHighlightRepository extends JpaRepository<TourHighlight, Long> {
}
