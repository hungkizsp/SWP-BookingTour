package com.tourbooking.booking.backend.security.filter;

import com.tourbooking.booking.backend.model.entity.SecurityLog;
import com.tourbooking.booking.backend.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Separate @Service so that @Async is applied to this bean, NOT to the Filter.
 * If @Async were placed directly on a method in BookingSecurityFilter,
 * Spring would create a JDK dynamic proxy for the filter, breaking injection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityLogRepository securityLogRepository;

    @Async
    public void log(String ip, Long userId, String userEmail, String endpoint,
                    String method, int statusCode, long responseTimeMs, String status) {
        try {
            SecurityLog entry = SecurityLog.builder()
                    .ipAddress(ip)
                    .userId(userId)
                    .userEmail(userEmail)
                    .endpoint(endpoint)
                    .method(method)
                    .statusCode(statusCode)
                    .responseTimeMs(responseTimeMs)
                    .status(status)
                    .build();
            securityLogRepository.save(entry);
        } catch (Exception e) {
            // Never let logging crash the application
            log.debug("[SECURITY AUDIT] Failed to persist log entry: {}", e.getMessage());
        }
    }
}
