package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
