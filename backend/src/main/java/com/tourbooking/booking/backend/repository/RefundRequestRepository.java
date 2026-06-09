package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findTopByBooking_IdOrderByCreatedAtDesc(Long bookingId);

    List<RefundRequest> findByBooking_IdIn(Collection<Long> bookingIds);
}
