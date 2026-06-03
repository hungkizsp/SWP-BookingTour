package com.tourbooking.booking.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tourbooking.booking.backend.model.entity.ChatEscalation;
import com.tourbooking.booking.backend.model.entity.enums.ChatEscalationStatus;

@Repository
public interface ChatEscalationRepository extends JpaRepository<ChatEscalation, Long> {
    Optional<ChatEscalation> findTopByCustomer_IdAndStatusNotInOrderByUpdatedAtDesc(Long customerId, Collection<ChatEscalationStatus> statuses);
    Optional<ChatEscalation> findTopByGuestIdAndStatusNotInOrderByUpdatedAtDesc(String guestId, Collection<ChatEscalationStatus> statuses);
    List<ChatEscalation> findByStatusInOrderByUpdatedAtDesc(Collection<ChatEscalationStatus> statuses);
}
