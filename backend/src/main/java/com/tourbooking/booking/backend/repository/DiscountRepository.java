package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    Optional<Discount> findByCode(String code);
    boolean existsByCode(String code);
    org.springframework.data.domain.Page<Discount> findByIsActiveTrue(org.springframework.data.domain.Pageable pageable);
}
