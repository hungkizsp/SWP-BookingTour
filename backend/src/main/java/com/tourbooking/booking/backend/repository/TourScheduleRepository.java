package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, Long> {
    
    // UC: Check if tour has schedules
    long countByTour_Id(Long tourId);

    // UC: Find all schedules with status OPEN
    @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN")
    List<TourSchedule> findAllOpen();

    // UC: Find schedules assigned to a specific guide
    List<TourSchedule> findByGuide_Id(Long guideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TourSchedule s WHERE s.id = :id")
    Optional<TourSchedule> findByIdWithLock(@Param("id") Long id);
}

