package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.AILog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AILogRepository extends JpaRepository<AILog, Long> {
    List<AILog> findTop50ByOrderByIdDesc();
}
