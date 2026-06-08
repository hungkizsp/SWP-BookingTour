package com.tourbooking.booking.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Pessimistic Write Lock — chống race condition khi nhiều staff cùng tiếp nhận */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChatSession s WHERE s.id = :id")
    Optional<ChatSession> findByIdWithLock(@Param("id") Long id);

    /** Tìm các phiên STAFF_CHATTING bị treo quá thời gian để auto-close */
    @Query("SELECT s FROM ChatSession s WHERE s.status = :status AND s.lastMessageAt < :cutoff")
    List<ChatSession> findByStatusAndLastMessageAtBefore(
            @Param("status") ChatSessionStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
