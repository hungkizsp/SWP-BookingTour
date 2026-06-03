package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourProgressLog;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TourProgressLogRepository extends JpaRepository<TourProgressLog, Long> {
    List<TourProgressLog> findByScheduleOrderByCreatedAtDesc(TourSchedule schedule);
}
