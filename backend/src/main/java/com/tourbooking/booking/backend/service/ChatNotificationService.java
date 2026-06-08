package com.tourbooking.booking.backend.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tourbooking.booking.backend.model.dto.response.ChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý SSE emitters cho hai loại event:
 * - "chat-notification" : thông báo escalation mới cho Staff dashboard
 * - "chat-message"      : tin nhắn real-time đến khách hàng / staff đang chat
 */
@Service
@Slf4j
public class ChatNotificationService {

    /** Emitters cho Staff dashboard (nhận escalation notifications) */
    private final List<SseEmitter> staffEmitters = new CopyOnWriteArrayList<>();

    /** Emitters cho khách hàng / staff đang trong phiên chat cụ thể */
    private final List<SseEmitter> chatEmitters = new CopyOnWriteArrayList<>();

    // ── Staff dashboard SSE ────────────────────────────────────────────────────

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> staffEmitters.remove(emitter));
        emitter.onTimeout(() -> staffEmitters.remove(emitter));
        emitter.onError(e -> staffEmitters.remove(emitter));
        staffEmitters.add(emitter);
        return emitter;
    }

    public void publish(ChatNotification notification) {
        broadcast(staffEmitters, emitter ->
                emitter.send(SseEmitter.event().name("chat-notification").data(notification)));
    }

    // ── Chat message SSE (real-time) ──────────────────────────────────────────

    /** Client (khách hàng hoặc staff đang chat) đăng ký nhận tin nhắn real-time */
    public SseEmitter subscribeToMessages() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> chatEmitters.remove(emitter));
        emitter.onTimeout(() -> chatEmitters.remove(emitter));
        emitter.onError(e -> chatEmitters.remove(emitter));
        chatEmitters.add(emitter);
        return emitter;
    }

    /**
     * Push tin nhắn mới đến TẤT CẢ client đang lắng nghe (broadcast).
     * Frontend lọc theo sessionId/userId/guestId ở phía client.
     */
    public void publishMessage(ChatMessageResponse message) {
        broadcast(chatEmitters, emitter ->
                emitter.send(SseEmitter.event().name("chat-message").data(message)));
    }

    /**
     * 1.2 — Push event "session-closed" khi Staff nhấn "Mark resolved".
     * Frontend bắt event này để disable ô nhập liệu và hiển thị thông báo kết thúc.
     *
     * @param sessionId ID phiên chat vừa được đóng
     */
    public void publishSessionClosed(Long sessionId) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                "type", "SESSION_CLOSED",
                "sessionId", sessionId,
                "status", "CLOSED",
                "message", "Cuộc hỗ trợ đã kết thúc bởi nhân viên."
        );
        broadcast(chatEmitters, emitter -> {
            emitter.send(SseEmitter.event().name("session-closed").data(payload));
            emitter.send(SseEmitter.event().name("chat-message").data(payload));
        });
    }

    /**
     * Bắn event thông báo staff đã join phiên chat thành công.
     */
    public void publishSessionAssigned(Long sessionId, String staffName) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                "type", "SESSION_ASSIGNED",
                "sessionId", sessionId,
                "status", "STAFF_CHATTING",
                "staffName", staffName,
                "message", "Nhân viên " + staffName + " đã tham gia cuộc trò chuyện."
        );
        broadcast(chatEmitters, emitter -> {
            emitter.send(SseEmitter.event().name("session-assigned").data(payload));
            emitter.send(SseEmitter.event().name("chat-message").data(payload));
        });
    }

    @FunctionalInterface
    private interface EmitterAction {
        void send(SseEmitter emitter) throws IOException;
    }

    private void broadcast(List<SseEmitter> emitters, EmitterAction action) {
        for (SseEmitter emitter : emitters) {
            try {
                action.send(emitter);
            } catch (Exception ex) {
                emitters.remove(emitter);
                completeQuietly(emitter);
                if (log.isDebugEnabled()) {
                    log.debug("Removed stale SSE emitter: {}", ex.getMessage());
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
