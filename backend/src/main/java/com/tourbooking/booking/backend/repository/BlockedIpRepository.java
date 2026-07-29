package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedIpRepository extends JpaRepository<BlockedIp, Long> {

    Optional<BlockedIp> findFirstByIpAddressAndBlockedUntilAfter(String ipAddress, LocalDateTime now);

    List<BlockedIp> findByBlockedUntilAfterOrderByCreatedAtDesc(LocalDateTime now);

    void deleteByBlockedUntilBefore(LocalDateTime now);

    /** Remove ALL active (and expired) block records for this identifier at once. */
    void deleteAllByIpAddress(String ipAddress);
}
