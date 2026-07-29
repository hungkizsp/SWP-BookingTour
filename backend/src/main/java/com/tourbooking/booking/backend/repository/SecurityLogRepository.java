package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    long countByCreatedAtAfterAndStatus(LocalDateTime after, String status);

    long countByCreatedAtAfter(LocalDateTime after);

    @Query("SELECT AVG(s.responseTimeMs) FROM SecurityLog s WHERE s.createdAt >= :after")
    Double avgResponseTimeAfter(@Param("after") LocalDateTime after);

    @Query(value = "SELECT TOP 10 ip_address, COUNT(*) as cnt FROM security_logs " +
                   "WHERE created_at >= :after " +
                   "GROUP BY ip_address ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findTopIps(@Param("after") LocalDateTime after);

    @Query(value = "SELECT CAST(created_at AS DATE) as day, COUNT(*) as total, " +
                   "SUM(CASE WHEN status = 'BLOCKED' THEN 1 ELSE 0 END) as blocked " +
                   "FROM security_logs WHERE created_at >= :after " +
                   "GROUP BY CAST(created_at AS DATE) ORDER BY CAST(created_at AS DATE)",
           nativeQuery = true)
    List<Object[]> findDailyStats(@Param("after") LocalDateTime after);

    @Query(value = "SELECT TOP(:limit) id, ip_address, user_id, user_email, endpoint, method, " +
                   "status_code, response_time_ms, status, created_at " +
                   "FROM security_logs ORDER BY created_at DESC",
           nativeQuery = true)
    List<Object[]> findRecentLogs(@Param("limit") int limit);

    @Query(value = "SELECT TOP(:limit) id, ip_address, user_id, user_email, endpoint, method, " +
                   "status_code, response_time_ms, status, created_at " +
                   "FROM security_logs WHERE status = :status ORDER BY created_at DESC",
           nativeQuery = true)
    List<Object[]> findRecentLogsByStatus(@Param("limit") int limit, @Param("status") String status);
}
