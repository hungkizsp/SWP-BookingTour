package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, Long> {

       // UC: Check if tour has schedules
       long countByTour_Id(Long tourId);

       @Query("SELECT s FROM TourSchedule s WHERE s.tour.id = :tourId " +
                     "AND s.status IN (com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN, com.tourbooking.booking.backend.model.entity.enums.TourStatus.SOLD_OUT) "
                     +
                     "AND (s.startDate > :currentDate OR " +
                     "(s.startDate = :currentDate AND s.departureTime > :currentTime)) " +
                     "ORDER BY s.startDate ASC, s.departureTime ASC")
       List<TourSchedule> findFutureSchedules(@Param("tourId") Long tourId,
                     @Param("currentDate") LocalDate currentDate,
                     @Param("currentTime") LocalTime currentTime);

       // UC: Find all schedules with status OPEN
       @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN")
       List<TourSchedule> findAllOpen();

       // UC: Find schedules assigned to a specific guide
       List<TourSchedule> findByGuide_Id(Long guideId);

       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT s FROM TourSchedule s WHERE s.id = :id")
       Optional<TourSchedule> findByIdWithLock(@Param("id") Long id);

       @org.springframework.data.jpa.repository.Modifying
       @Query("UPDATE TourSchedule ts SET ts.availableSlots = ts.availableSlots + :people WHERE ts.id = :scheduleId")
       void releaseAvailableSlots(@Param("scheduleId") Long scheduleId, @Param("people") Integer people);

       // ── Scheduler queries for status transitions ──────────────────────────

       /**
        * Find OPEN or SOLD_OUT schedules whose booking deadline has passed.
        * These should transition to BOOKING_CLOSED.
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status IN :statuses AND " +
                     "((s.bookingDeadline IS NOT NULL AND s.bookingDeadline < :now) OR " +
                     " (s.bookingDeadline IS NULL AND s.startDate < :today))")
       List<TourSchedule> findSchedulesPastDeadline(
                     @Param("statuses") List<TourStatus> statuses,
                     @Param("now") LocalDateTime now,
                     @Param("today") LocalDate today);

       /**
        * Find schedules that should be IN_PROGRESS (departure datetime has passed).
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status IN :statuses AND s.startDate <= :today")
       List<TourSchedule> findSchedulesPastDeparture(
                     @Param("statuses") List<TourStatus> statuses,
                     @Param("today") LocalDate today);

       /**
        * Find IN_PROGRESS schedules that should be COMPLETED (return datetime has
        * passed).
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.IN_PROGRESS AND s.endDate <= :today")
       List<TourSchedule> findSchedulesPastReturn(@Param("today") LocalDate today);

       /**
        * Find all schedules with given statuses.
        */
       List<TourSchedule> findByStatusIn(List<TourStatus> statuses);

       // ── Operational Readiness Queries ─────────────────────────────────────

       /**
        * Find OPEN schedules where guideId IS NULL and startDate is on or after today.
        * Used by the early-warning alert system (24H/12H/6H/2H windows).
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN "
                     +
                     "AND s.guide IS NULL AND s.startDate >= :today")
       List<TourSchedule> findOpenSchedulesWithNoGuide(@Param("today") LocalDate today);

       /**
        * Find OPEN schedules with no guide where departure datetime is within 1 hour
        * from now.
        * These should be transitioned to PENDING_GUIDE.
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.OPEN "
                     +
                     "AND s.guide IS NULL AND s.startDate <= :today")
       List<TourSchedule> findOpenNoGuideSchedulesOnOrBeforeDate(@Param("today") LocalDate today);

       /**
        * Find PENDING_GUIDE schedules whose departure datetime has passed.
        * These should be auto-cancelled.
        */
       @Query("SELECT s FROM TourSchedule s WHERE s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.PENDING_GUIDE "
                     +
                     "AND s.startDate <= :today")
       List<TourSchedule> findPendingGuideSchedulesPastDeparture(@Param("today") LocalDate today);

       /** Count schedules with PENDING_GUIDE status. */
       long countByStatus(TourStatus status);
}
