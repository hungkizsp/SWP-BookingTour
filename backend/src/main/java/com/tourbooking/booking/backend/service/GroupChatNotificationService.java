package com.tourbooking.booking.backend.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tourbooking.booking.backend.model.dto.response.GroupChatMessageResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * SSE cho nhóm chat theo từng TourSchedule. Khác với ChatNotificationService (broadcast toàn cục
 * rồi lọc ở client), ở đây mỗi scheduleId có kênh emitter riêng nên chỉ push tới đúng nhóm liên quan.
 */
@Service
@Slf4j
public class GroupChatNotificationService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersBySchedule = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long scheduleId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        CopyOnWriteArrayList<SseEmitter> emitters = emittersBySchedule.computeIfAbsent(
                scheduleId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(Long scheduleId, GroupChatMessageResponse message) {
        List<SseEmitter> emitters = emittersBySchedule.get(scheduleId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("group-chat-message").data(message));
            } catch (Exception ex) {
                emitters.remove(emitter);
                completeQuietly(emitter);
                if (log.isDebugEnabled()) {
                    log.debug("Removed stale group-chat SSE emitter (schedule {}): {}", scheduleId, ex.getMessage());
                }
            }
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // Client already disconnected.
        }
    }
}
