package com.tourbooking.booking.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourbooking.booking.backend.model.entity.BlockedIp;
import com.tourbooking.booking.backend.repository.BlockedIpRepository;
import com.tourbooking.booking.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security filter implementing:
 *  1. IP Blacklist check  (HTTP 403 if blocked)
 *  2. Booking rate limit  max 10 req/min per IP (HTTP 429)
 *  3. General rate limit  suspicious at >100 req/min, blocked at >200 req/min
 *  4. Async audit logging via SecurityAuditService (separate @Service/@Async bean)
 *
 * NOTE: Do NOT place @Async on methods in this class. That would cause Spring to
 * wrap it in a JDK dynamic proxy which breaks constructor injection in SecurityConfig.
 * Use SecurityAuditService for all async work instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSecurityFilter extends OncePerRequestFilter {

    private static final int  BOOKING_RATE_LIMIT   = 10;    // per minute — demo: 10 POST /bookings
    private static final int  SUSPICIOUS_THRESHOLD = 15;    // per minute on sensitive paths — mark SUSPICIOUS
    private static final int  BLOCK_THRESHOLD      = 20;    // per minute on sensitive paths — auto-blacklist
    private static final long WINDOW_MS            = 60_000L;
    private static final long BLOCK_DURATION_MIN   = 10L;

    /**
     * Only these "sensitive" paths are counted in the general rate limiter.
     * Normal SPA browsing calls each of these at most once per page navigation,
     * so a low BLOCK_THRESHOLD (3) is safe for normal users but immediately
     * catches Postman rapid-fire spam (4+ calls to the same path = blocked).
     */
    private static final java.util.Set<String> RATE_LIMITED_PATHS = java.util.Set.of(
            "/api/v1/auth/me"
            // NOTE: /auth/login is intentionally excluded — the SPA calls login once
            // and then immediately fetches /auth/me multiple times for role/profile hydration.
            // Counting login itself would cause immediate false-positive blocks on normal usage.
    );

    private final ConcurrentHashMap<String, Queue<Long>> bookingCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<Long>> generalCounters  = new ConcurrentHashMap<>();
    /** Prevents multiple concurrent threads from each calling blockIp() for the same identifier. */
    private final ConcurrentHashMap<String, Boolean>     blockingInProgress = new ConcurrentHashMap<>();

    private final BlockedIpRepository  blockedIpRepository;
    private final SecurityAuditService securityAuditService;  // @Async lives here
    private final ObjectMapper         objectMapper;
    private final JwtService           jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip     = resolveClientIp(request);
        String path   = request.getRequestURI();
        String method = request.getMethod();
        long   start  = System.currentTimeMillis();

        // 1. Resolve identifier (User Email and ID if logged in)
        String token = resolveToken(request);
        String userEmail = null;
        Long userId = null;
        String role = null;
        if (token != null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                userEmail = claims.getSubject();
                role = claims.get("role", String.class);
                Object uidObj = claims.get("userId");
                if (uidObj instanceof Number) {
                    userId = ((Number) uidObj).longValue();
                }
            } catch (Exception ignored) {}
        }
        
        // If not logged in, skip rate limit / block checks entirely (anonymous/public traffic never blocked)
        if (userEmail == null) {
            filterChain.doFilter(request, response);
            securityAuditService.log(ip, null, null, path, method, response.getStatus(),
                    System.currentTimeMillis() - start, "NORMAL");
            return;
        }

        // Bypass security rate limit & block checks for ADMIN, STAFF, and GUIDE roles
        if (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("STAFF") || role.equalsIgnoreCase("GUIDE") ||
                             role.equalsIgnoreCase("ROLE_ADMIN") || role.equalsIgnoreCase("ROLE_STAFF") || role.equalsIgnoreCase("ROLE_GUIDE"))) {
            filterChain.doFilter(request, response);
            securityAuditService.log(ip, userId, userEmail, path, method, response.getStatus(),
                    System.currentTimeMillis() - start, "NORMAL");
            return;
        }

        String identifier = "USER:" + userEmail;


        // ── 2. Check blacklist (Bypass security dashboard API to prevent locking out admin)
        if (isBlocked(identifier) && !path.startsWith("/api/v1/admin/security")) {
            log.warn("[SECURITY] Blocked user attempted access: {} → {}", identifier, path);
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    1030, "Tài khoản của bạn tạm thời bị khóa do gửi yêu cầu quá nhanh.");
            securityAuditService.log(ip, userId, userEmail, path, method, 403,
                    System.currentTimeMillis() - start, "BLOCKED");
            return;
        }

        // ── 3. Booking-specific rate limit ──────────────────────────
        if ("POST".equalsIgnoreCase(method) && path.matches("/api/v1/bookings(?:/.*)?")) {
            int cnt = countAndSlide(bookingCounters, identifier);
            if (cnt > BOOKING_RATE_LIMIT) {
                log.warn("[SECURITY] Booking rate limit for {}: {} req/min", identifier, cnt);
                writeError(response, 429, 1028,
                        "Tần suất đặt tour quá nhanh. Tối đa 10 request/phút. Vui lòng thử lại sau.");
                securityAuditService.log(ip, userId, userEmail, path, method, 429,
                        System.currentTimeMillis() - start, "BLOCKED");
                return;
            }
        }

        // ── 4. General rate limit — only on RATE_LIMITED_PATHS ───────────────────
        // Normal SPA calls auth/me once per page load (1 req/min) — safe.
        // Postman rapid-fire spam sends 4+ auth/me calls quickly — triggers block.
        if (RATE_LIMITED_PATHS.contains(path)) {
            int generalCnt = countAndSlide(generalCounters, identifier);
            log.info("[SECURITY] Sensitive path hit: {} | user={} | count={}/{}", path, identifier, generalCnt, BLOCK_THRESHOLD);
            if (generalCnt > BLOCK_THRESHOLD) {
                // putIfAbsent returns null only for the FIRST thread — prevents duplicate blockIp() calls
                boolean isFirstToBlock = blockingInProgress.putIfAbsent(identifier, Boolean.TRUE) == null;
                if (isFirstToBlock) {
                    log.warn("[SECURITY] Auto-blacklisting {} ({} req/min on sensitive path)", identifier, generalCnt);
                    blockIp(identifier, "Tự động chặn: vượt " + BLOCK_THRESHOLD + " request/phút đến endpoint nhạy cảm");
                }
                writeError(response, 429, 1028, "Quá nhiều request. Tài khoản của bạn đã bị chặn trong 10 phút.");
                securityAuditService.log(ip, userId, userEmail, path, method, 429,
                        System.currentTimeMillis() - start, "BLOCKED");
                return;
            }
        }

        // For non-sensitive paths, logStatus is always NORMAL (not counted in rate limit)
        // For sensitive paths that passed the rate limit check, also NORMAL
        String logStatus = "NORMAL";

        // ── 5. Proceed ─────────────────────────────────────────────
        filterChain.doFilter(request, response);

        securityAuditService.log(ip, userId, userEmail, path, method, response.getStatus(),
                System.currentTimeMillis() - start, logStatus);
    }




    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isBlocked(String ip) {
        try {
            return blockedIpRepository
                    .findFirstByIpAddressAndBlockedUntilAfter(ip, LocalDateTime.now())
                    .isPresent();
        } catch (Exception e) {
            log.warn("[SECURITY] Could not check blocked IP: {}", e.getMessage());
            return false;
        }
    }

    private void blockIp(String ip, String reason) {
        try {
            BlockedIp block = BlockedIp.builder()
                    .ipAddress(ip)
                    .reason(reason)
                    .blockedUntil(LocalDateTime.now().plusMinutes(BLOCK_DURATION_MIN))
                    .build();
            blockedIpRepository.save(block);
        } catch (Exception e) {
            log.error("[SECURITY] Failed to save blocked IP: {}", e.getMessage());
        }
    }

    /**
     * Called by admin unblock action to reset the in-memory rate-limit counters
     * for an identifier (e.g. "USER:customer@gmail.com"), so that the user
     * is not immediately re-blocked on their very next request.
     */
    public void resetCounter(String identifier) {
        generalCounters.remove(identifier);
        bookingCounters.remove(identifier);
        blockingInProgress.remove(identifier);   // allow re-blocking in the future if needed
        log.info("[SECURITY] In-memory counters reset for identifier: {}", identifier);
    }

    private int countAndSlide(ConcurrentHashMap<String, Queue<Long>> map, String ip) {
        long now = System.currentTimeMillis();
        Queue<Long> ts = map.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (ts) {
            ts.add(now);
            while (!ts.isEmpty() && now - ts.peek() > WINDOW_MS) ts.poll();
            return ts.size();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int httpStatus,
                             int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = Map.of(
                "code", code,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private boolean isWhitelistedPathForLocalhost(String ip, String path) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/auth/");
        }
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7).trim();
        }
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        return null;
    }
}


