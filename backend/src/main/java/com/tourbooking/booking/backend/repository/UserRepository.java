package com.tourbooking.booking.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    java.util.List<User> findByRole(com.tourbooking.booking.backend.model.entity.enums.UserRole role);
    
    java.util.List<User> findByIsActiveFalseAndCreatedAtBefore(java.time.LocalDateTime cutoff);

    long countByCurrentSessionIdIsNotNull();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
}

