package com.tourbooking.booking.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.Newsletter;

import java.util.Optional;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<Newsletter> findByEmailIgnoreCase(String email);
}
