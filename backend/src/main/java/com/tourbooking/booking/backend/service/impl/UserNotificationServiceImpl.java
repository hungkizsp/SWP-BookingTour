package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.dto.response.UserNotificationResponse;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.UserNotification;
import com.tourbooking.booking.backend.repository.UserNotificationRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final long EMITTER_TIMEOUT_MS = 5L * 60L * 1000L;

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void notify(Long userId, String title, String message, String type, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        UserNotification notif = new UserNotification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        notif.setLink(link);
        notif.setRead(false);
        notificationRepository.save(notif);

        pushToUser(userId, toResponse(notif));
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        // send current unread count on connect
        try {
            long unread = notificationRepository.countByUser_IdAndIsReadFalse(userId);
            emitter.send(SseEmitter.event().name("unread-count").data(unread));
        } catch (Exception ignored) {}

        return emitter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(50)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long notifId, Long userId) {
        notificationRepository.findByIdAndUser_Id(notifId, userId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
            pushUnreadCount(userId);
        });
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        pushUnreadCount(userId);
    }

    @Async
    protected void pushToUser(Long userId, UserNotificationResponse response) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(response));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    private void pushUnreadCount(Long userId) {
        long count = notificationRepository.countByUser_IdAndIsReadFalse(userId);
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("unread-count").data(count));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByUser.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emittersByUser.remove(userId);
        }
    }

    private UserNotificationResponse toResponse(UserNotification n) {
        return UserNotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .link(n.getLink())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
