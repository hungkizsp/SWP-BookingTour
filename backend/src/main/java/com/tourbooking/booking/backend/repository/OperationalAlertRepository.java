package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.OperationalAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperationalAlertRepository extends JpaRepository<OperationalAlert, Long> {

    /** Used by the scheduler to check idempotency before sending an alert. */
    boolean existsByScheduleIdAndAlertWindow(Long scheduleId, String alertWindow);

    /** Returns the most recent alert for a schedule+window combo (for read APIs). */
    Optional<OperationalAlert> findByScheduleIdAndAlertWindow(Long scheduleId, String alertWindow);

    /** Deletes all alert records for a schedule — used when a zero-booking schedule is purged. */
    void deleteByScheduleId(Long scheduleId);
}
