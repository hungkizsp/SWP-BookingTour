package com.tourbooking.booking.backend.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AuthSessionNotificationService {
    SseEmitter subscribe(String token);

    void notifySessionInvalidated(String email, String message);
    void notifyOthers(String email, String currentSessionId, String message);
    void heartbeat();
}
