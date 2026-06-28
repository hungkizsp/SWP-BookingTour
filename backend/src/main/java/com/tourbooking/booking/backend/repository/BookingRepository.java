package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    long countByUserId(Long userId);
    List<Booking> findByScheduleId(Long scheduleId);
    List<Booking> findByUser_IdAndStatusIn(Long userId, List<BookingStatus> statuses);
    List<Booking> findByStatus(BookingStatus status);
    org.springframework.data.domain.Page<Booking> findByStatus(BookingStatus status, org.springframework.data.domain.Pageable pageable);
    List<Booking> findByBookingDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Booking> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    long countByStatus(BookingStatus status);
    
    // UC18: Booking History with filters, search, and pagination
    /**
     * Find bookings for a customer with JOIN FETCH to prevent N+1 queries
     * Fetches user, schedule, tour, payment in single query
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.schedule s " +
           "LEFT JOIN FETCH s.tour t " +
           "LEFT JOIN FETCH b.payment " +
           "WHERE b.user.id = :customerId")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Long customerId);
    
    /**
     * Find bookings with filters and search
     * Search applies to: tour name, booking reference (ID), destination
     * Filters: status list, date range, price range
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.schedule s " +
           "LEFT JOIN FETCH s.tour t " +
           "WHERE b.user.id = :customerId " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(t.tourName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     CAST(b.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "     LOWER(t.endLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(t.startLocation) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:statuses IS NULL OR b.status IN :statuses) " +
           "AND (:dateFrom IS NULL OR s.startDate >= :dateFrom) " +
           "AND (:dateTo IS NULL OR s.startDate <= :dateTo) " +
           "AND (:priceMin IS NULL OR b.totalPrice >= :priceMin) " +
           "AND (:priceMax IS NULL OR b.totalPrice <= :priceMax)")
    org.springframework.data.domain.Page<Booking> findBookingHistoryWithFilters(
        @Param("customerId") Long customerId,
        @Param("search") String search,
        @Param("statuses") List<BookingStatus> statuses,
        @Param("dateFrom") java.time.LocalDate dateFrom,
        @Param("dateTo") java.time.LocalDate dateTo,
        @Param("priceMin") java.math.BigDecimal priceMin,
        @Param("priceMax") java.math.BigDecimal priceMax,
        org.springframework.data.domain.Pageable pageable
    );
    
    /**
     * Count bookings by customer and status for statistics
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :customerId AND b.status = :status")
    long countByUserIdAndStatus(@Param("customerId") Long customerId, @Param("status") BookingStatus status);
    
    /**
     * Calculate total spent by customer
     */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.user.id = :customerId AND b.status IN :statuses")
    java.math.BigDecimal sumTotalPriceByUserIdAndStatusIn(@Param("customerId") Long customerId, @Param("statuses") List<BookingStatus> statuses);

    // UC47: Tìm booking PENDING chưa có payment thành công, đã quá hạn (tạo trước cutoff)
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :cutoff " +
           "AND (b.payment IS NULL OR b.payment.status <> 'SUCCESS')")
    List<Booking> findPendingOnlineUnpaidBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_CASH' AND b.createdAt < :cutoff " +
           "AND (b.payment IS NULL OR b.payment.status <> 'SUCCESS')")
    List<Booking> findPendingCashUnpaidBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT b FROM Booking b WHERE b.status = com.tourbooking.booking.backend.model.entity.enums.BookingStatus.PENDING " +
           "AND b.createdAt < :cutoff AND b.payment.paymentMethod = 'PAYOS'")
    List<Booking> findPendingPayosBookingsBefore(@Param("cutoff") LocalDateTime cutoff);

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

    // ── Operational Scheduler Queries ─────────────────────────────────────

    /** Find all CONFIRMED bookings for a specific schedule — used by the auto-cancellation refund flow. */
    @Query("SELECT b FROM Booking b WHERE b.schedule.id = :scheduleId " +
           "AND b.status = com.tourbooking.booking.backend.model.entity.enums.BookingStatus.CONFIRMED")
    List<Booking> findConfirmedByScheduleId(@Param("scheduleId") Long scheduleId);

    /** Count bookings with REFUNDED status (caused by CANCELLED_BY_OPERATOR). */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = com.tourbooking.booking.backend.model.entity.enums.BookingStatus.REFUNDED " +
           "AND EXISTS (SELECT 1 FROM TourSchedule s WHERE s.id = b.schedule.id " +
           "AND s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.CANCELLED_BY_OPERATOR)")
    long countOperatorCancelledRefundedBookings();

    /** Sum total refund amounts for bookings caused by CANCELLED_BY_OPERATOR. */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = com.tourbooking.booking.backend.model.entity.enums.BookingStatus.REFUNDED " +
           "AND EXISTS (SELECT 1 FROM TourSchedule s WHERE s.id = b.schedule.id " +
           "AND s.status = com.tourbooking.booking.backend.model.entity.enums.TourStatus.CANCELLED_BY_OPERATOR)")
    java.math.BigDecimal sumOperatorCancelledRefundAmounts();
}

