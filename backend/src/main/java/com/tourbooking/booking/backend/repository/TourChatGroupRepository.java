package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourChatGroupRepository extends JpaRepository<TourChatGroup, Long> {

    Optional<TourChatGroup> findBySchedule_Id(Long scheduleId);

    @Query("SELECT g FROM TourChatGroup g JOIN g.members m WHERE m.user.id = :userId AND g.isActive = true")
    List<TourChatGroup> findActiveGroupsByUserId(Long userId);
}
