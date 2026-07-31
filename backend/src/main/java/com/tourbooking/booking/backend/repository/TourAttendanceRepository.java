package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourAttendanceRepository extends JpaRepository<TourAttendance, Long> {
    List<TourAttendance> findByScheduleId(Long scheduleId);
    List<TourAttendance> findByScheduleIdAndStatus(Long scheduleId, com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus status);
    List<TourAttendance> findByStatus(com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus status);
}
