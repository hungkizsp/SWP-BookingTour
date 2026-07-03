package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourChatGroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourChatGroupMessageRepository extends JpaRepository<TourChatGroupMessage, Long> {
    Page<TourChatGroupMessage> findByGroupId(Long groupId, Pageable pageable);
}
