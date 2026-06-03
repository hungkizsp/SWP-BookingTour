package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByBookingDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Booking> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    long countByStatus(BookingStatus status);

    // UC47: Tìm booking PENDING chưa có payment thành công, đã quá hạn (tạo trước cutoff)
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :cutoff " +
           "AND (b.payment IS NULL OR b.payment.status <> 'SUCCESS')")
    List<Booking> findPendingUnpaidBefore(@Param("cutoff") LocalDateTime cutoff);

    // UC50: Đếm booking theo status trong tháng
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status " +
           "AND b.createdAt >= :from AND b.createdAt < :to")
    long countByStatusAndCreatedAtBetween(@Param("status") BookingStatus status,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    // UC50: Tổng doanh thu CONFIRMED trong tháng
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.createdAt >= :from AND b.createdAt < :to")
    java.math.BigDecimal sumRevenueConfirmedBetween(@Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    // UC50: Lấy tất cả booking trong tháng (cho báo cáo)
    @Query("SELECT b FROM Booking b WHERE b.createdAt >= :from AND b.createdAt < :to")
    List<Booking> findAllInPeriod(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
