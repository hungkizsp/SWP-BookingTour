package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.TourChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourChatGroupMemberRepository extends JpaRepository<TourChatGroupMember, Long> {
    Optional<TourChatGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    List<TourChatGroupMember> findByGroupId(Long groupId);
}
