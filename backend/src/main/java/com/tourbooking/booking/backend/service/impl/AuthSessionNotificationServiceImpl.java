package com.tourbooking.booking.backend.service.impl;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tourbooking.booking.backend.security.JwtService;
import com.tourbooking.booking.backend.service.AuthSessionNotificationService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthSessionNotificationServiceImpl implements AuthSessionNotificationService {

    private static final long EMITTER_TIMEOUT_MS = 1L * 60L * 1000L; // 1 phút

    private final JwtService jwtService;
    private final SessionValidatorService sessionValidatorService;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ClientConnection>> connections = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String token) {
        Claims claims = jwtService.parseClaims(token);
        String email = claims.getSubject();
        Object sessionIdObj = claims.get("sessionId");
        String sessionId = sessionIdObj != null ? sessionIdObj.toString() : null;

        if (email == null || sessionId == null) {
            SseEmitter errorEmitter = new SseEmitter(1000L);
            errorEmitter.completeWithError(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."));
            return errorEmitter;
        }

        // DB validation
        try {
            sessionValidatorService.validateSession(email, sessionId);
        } catch (ResponseStatusException e) {
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(e.getReason() != null ? e.getReason()
                        : "Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại.");
                errorEmitter.complete();
            } catch (Exception ignored) {
            }
            return errorEmitter;
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        ClientConnection connection = new ClientConnection(sessionId, emitter);

        connections.computeIfAbsent(email, key -> new CopyOnWriteArrayList<>()).add(connection);

        Runnable cleanup = () -> removeConnection(email, connection);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        return emitter;
    }

    @Async
    @Override
    public void notifySessionInvalidated(String email, String message) {
        notifyOthers(email, null, message);
    }

    @Async
    @Override
    public void notifyOthers(String email, String currentSessionId, String message) {
        CopyOnWriteArrayList<ClientConnection> currentConnections = connections.get(email);
        if (currentConnections == null || currentConnections.isEmpty()) {
            return;
        }

        for (ClientConnection connection : currentConnections) {
            if (currentSessionId != null && currentSessionId.equals(connection.getSessionId())) {
                continue;
            }

            try {
                connection.getEmitter().send(SseEmitter.event()
                        .name("session_invalidated")
                        .data(Objects.requireNonNullElse(message, "Phiên làm việc hết hạn.")));
                connection.getEmitter().complete();
            } catch (Exception ignored) {
            } finally {
                removeConnection(email, connection);
            }
        }
    }

    @Override
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    public void heartbeat() {
        connections.forEach((email, currentConnections) -> {
            java.util.Iterator<ClientConnection> it = currentConnections.iterator();
            while (it.hasNext()) {
                ClientConnection conn = it.next();
                try {
                    conn.getEmitter().send(SseEmitter.event().name("ping").data("hb"));
                } catch (Exception e) {
                    try {
                        conn.getEmitter().complete();
                    } catch (Exception ignored) {}
                    currentConnections.remove(conn);
                }
            }
            if (currentConnections.isEmpty()) {
                connections.remove(email);
            }
        });
    }

    private void removeConnection(String email, ClientConnection connection) {
        CopyOnWriteArrayList<ClientConnection> current = connections.get(email);
        if (current != null) {
            current.remove(connection);
            if (current.isEmpty()) {
                connections.remove(email);
            }
        }
    }

    private static class ClientConnection {
        private final String sessionId;
        private final SseEmitter emitter;

        public ClientConnection(String sessionId, SseEmitter emitter) {
            this.sessionId = sessionId;
            this.emitter = emitter;
        }

        public String getSessionId() { return sessionId; }
        public SseEmitter getEmitter() { return emitter; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientConnection that = (ClientConnection) o;
            return Objects.equals(emitter, that.emitter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(emitter);
        }
    }
}
