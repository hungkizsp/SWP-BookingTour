package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.BlockedIp;
import com.tourbooking.booking.backend.repository.BlockedIpRepository;
import com.tourbooking.booking.backend.repository.SecurityLogRepository;
import com.tourbooking.booking.backend.security.filter.BookingSecurityFilter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Transactional
@RestController
@RequestMapping("/api/v1/admin/security")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
public class SecurityAnalyticsController {

    private final SecurityLogRepository securityLogRepository;
    private final BlockedIpRepository blockedIpRepository;
    private final BookingSecurityFilter bookingSecurityFilter;

    /**
     * GET /api/v1/admin/security/stats
     * Returns key metrics for the security dashboard summary cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        LocalDateTime since1h  = LocalDateTime.now().minusHours(1);

        long totalToday      = securityLogRepository.countByCreatedAtAfter(since24h);
        long suspiciousToday = securityLogRepository.countByCreatedAtAfterAndStatus(since24h, "SUSPICIOUS");
        long blockedToday    = securityLogRepository.countByCreatedAtAfterAndStatus(since24h, "BLOCKED");
        long activeBlocked   = blockedIpRepository.findByBlockedUntilAfterOrderByCreatedAtDesc(LocalDateTime.now()).size();
        Double avgResponse   = securityLogRepository.avgResponseTimeAfter(since1h);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequestsToday", totalToday);
        stats.put("suspiciousRequestsToday", suspiciousToday);
        stats.put("blockedRequestsToday", blockedToday);
        stats.put("activeBlockedIps", activeBlocked);
        stats.put("avgResponseTimeMs", avgResponse != null ? Math.round(avgResponse) : 0);
        stats.put("generatedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/v1/admin/security/charts
     * Returns chart data: daily request/block counts and top abusive IPs.
     */
    @GetMapping("/charts")
    public ResponseEntity<Map<String, Object>> getCharts() {
        LocalDateTime since7d = LocalDateTime.now().minusDays(7);

        // Daily stats: [date, totalCount, blockedCount]
        List<Object[]> rawDaily = securityLogRepository.findDailyStats(since7d);
        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (Object[] row : rawDaily) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", row[0] != null ? row[0].toString() : "");
            point.put("total", row[1]);
            point.put("blocked", row[2]);
            dailyData.add(point);
        }

        // Top abusive IPs (last 24h)
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        List<Object[]> rawTopIps = securityLogRepository.findTopIps(since24h);
        List<Map<String, Object>> topIps = new ArrayList<>();
        int limit = Math.min(rawTopIps.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = rawTopIps.get(i);
            Map<String, Object> ipEntry = new LinkedHashMap<>();
            ipEntry.put("ip", row[0]);
            ipEntry.put("count", row[1]);
            topIps.add(ipEntry);
        }

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("dailyStats", dailyData);
        charts.put("topAbusiveIps", topIps);

        return ResponseEntity.ok(charts);
    }

    /**
     * GET /api/v1/admin/security/blocked-ips
     * Returns list of currently active blocked IPs.
     */
    @GetMapping("/blocked-ips")
    public ResponseEntity<List<BlockedIp>> getBlockedIps() {
        List<BlockedIp> active = blockedIpRepository
                .findByBlockedUntilAfterOrderByCreatedAtDesc(LocalDateTime.now());
        return ResponseEntity.ok(active);
    }

    /**
     * DELETE /api/v1/admin/security/blocked-ips/{id}
     * Admin manually unblocks an IP by deleting the block record.
     */
    @DeleteMapping("/blocked-ips/{id}")
    public ResponseEntity<Map<String, String>> unblockIp(@PathVariable Long id) {
        if (!blockedIpRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Read identifier BEFORE deleting
        String identifier = blockedIpRepository.findById(id)
                .map(b -> b.getIpAddress())
                .orElse(null);
        // Delete ALL block records for this identifier (race condition may have created duplicates)
        if (identifier != null) {
            blockedIpRepository.deleteAllByIpAddress(identifier);
            // Reset in-memory sliding-window counters so the user is NOT immediately re-blocked
            bookingSecurityFilter.resetCounter(identifier);
            log.info("[SECURITY] Admin unblocked all records for identifier={}", identifier);
        } else {
            blockedIpRepository.deleteById(id);
        }
        return ResponseEntity.ok(Map.of("message", "Tài khoản đã được bỏ chặn thành công."));
    }

    /**
     * DELETE /api/v1/admin/security/blocked-ips/cleanup
     * Clean up expired block records from the database.
     */
    @DeleteMapping("/blocked-ips/cleanup")
    public ResponseEntity<Map<String, String>> cleanupExpiredBlocks() {
        blockedIpRepository.deleteByBlockedUntilBefore(LocalDateTime.now());
        return ResponseEntity.ok(Map.of("message", "Đã xóa các bản ghi chặn IP đã hết hạn."));
    }

    /**
     * GET /api/v1/admin/security/logs?limit=100&status=BLOCKED
     * Returns recent audit log entries for the security log table on the dashboard.
     * Optional param: status (ALL | NORMAL | SUSPICIOUS | BLOCKED)
     */
    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "ALL") String status) {

        List<Object[]> raw = "ALL".equalsIgnoreCase(status)
                ? securityLogRepository.findRecentLogs(Math.min(limit, 500))
                : securityLogRepository.findRecentLogsByStatus(Math.min(limit, 500), status.toUpperCase());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",             row[0]);
            entry.put("ipAddress",      row[1]);
            entry.put("userId",         row[2]);
            entry.put("userEmail",      row[3]);
            entry.put("endpoint",       row[4]);
            entry.put("method",         row[5]);
            entry.put("statusCode",     row[6]);
            entry.put("responseTimeMs", row[7]);
            entry.put("status",         row[8]);
            entry.put("createdAt",      row[9] != null ? row[9].toString() : null);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }
}
