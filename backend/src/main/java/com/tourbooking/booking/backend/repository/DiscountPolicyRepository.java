package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.DiscountPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountPolicyRepository extends JpaRepository<DiscountPolicy, Long> {
    Optional<DiscountPolicy> findByPassengerType(String passengerType);
}
