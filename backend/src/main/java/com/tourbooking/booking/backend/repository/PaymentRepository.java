package com.tourbooking.booking.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.Payment;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = :status AND p.paymentDate >= :start AND p.paymentDate <= :end")
    java.math.BigDecimal sumAmountByStatusAndDateBetween(
            @Param("status") com.tourbooking.booking.backend.model.entity.enums.PaymentStatus status,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = com.tourbooking.booking.backend.model.entity.enums.PaymentStatus.SUCCESS AND p.paymentDate >= :start")
    Double sumSuccessfulPaymentsAfter(@Param("start") LocalDateTime start);

    Optional<Payment> findByTransactionCode(String transactionCode);

    Optional<Payment> findFirstByBooking_IdAndStatusOrderByPaymentDateDesc(Long bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBooking_IdOrderByPaymentDateDesc(Long bookingId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.status = :status
              AND p.paymentMethod = :paymentMethod
              AND COALESCE(p.paymentDate, p.createdAt) >= :start
              AND COALESCE(p.paymentDate, p.createdAt) <= :end
            """)
    List<Payment> findPendingPayOsInRange(
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT p FROM Payment p
            WHERE COALESCE(p.paymentDate, p.createdAt) >= :start
              AND COALESCE(p.paymentDate, p.createdAt) <= :end
            """)
    List<Payment> findInDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
