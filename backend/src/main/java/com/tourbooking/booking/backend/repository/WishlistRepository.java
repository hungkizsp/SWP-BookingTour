package com.tourbooking.booking.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.Wishlist;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    boolean existsByUserIdAndTourId(Long userId, Long tourId);
    void deleteByUserIdAndTourId(Long userId, Long tourId);
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);
    Optional<Wishlist> findByUserIdAndTourId(Long userId, Long tourId);
}
