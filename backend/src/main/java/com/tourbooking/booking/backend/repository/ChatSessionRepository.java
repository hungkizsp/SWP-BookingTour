package com.tourbooking.booking.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tourbooking.booking.backend.model.entity.ChatSession;
import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findTopByUser_IdOrderByLastMessageAtDesc(Long userId);
    Optional<ChatSession> findTopByGuestIdOrderByLastMessageAtDesc(String guestId);
    List<ChatSession> findByStatusOrderByLastMessageAtDesc(ChatSessionStatus status);
    long countByStatus(ChatSessionStatus status);
    List<ChatSession> findByStatusInOrderByLastMessageAtDesc(List<ChatSessionStatus> statuses);
}
