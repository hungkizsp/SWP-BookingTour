package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findFirstByCategoryNameContainingIgnoreCase(String name);
}
