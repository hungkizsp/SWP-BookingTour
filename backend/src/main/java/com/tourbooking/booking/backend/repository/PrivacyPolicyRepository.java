package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.PrivacyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivacyPolicyRepository extends JpaRepository<PrivacyPolicy, Long> {
    List<PrivacyPolicy> findByIsActiveTrue();
}
