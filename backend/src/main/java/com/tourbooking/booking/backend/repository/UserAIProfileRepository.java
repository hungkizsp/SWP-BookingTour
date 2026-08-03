package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.UserAIProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAIProfileRepository extends JpaRepository<UserAIProfile, Long> {
    Optional<UserAIProfile> findByUserId(Long userId);
}
