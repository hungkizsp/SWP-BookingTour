package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.UserNotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface UserNotificationService {

    void notify(Long userId, String title, String message, String type, String link);

    SseEmitter subscribe(Long userId);

    List<UserNotificationResponse> getNotifications(Long userId);

    long countUnread(Long userId);

    void markRead(Long notifId, Long userId);

    void markAllRead(Long userId);
}
