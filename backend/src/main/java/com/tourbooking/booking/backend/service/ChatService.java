package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.ChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatSessionStatusResponse;
import com.tourbooking.booking.backend.model.entity.ChatSession;

import java.util.List;

public interface ChatService {
    ChatMessageResponse sendMessage(ChatMessageRequest request);

    /**
     * If userId is null: check guestId.
     */
    List<ChatMessageResponse> getConversation(Long userId, String guestId);

    void escalateToHuman(Long userId, String guestId);

    List<ChatSession> getActiveSessionsForStaff();

    void closeSession(Long userId, String guestId);

    /**
     * 1.1 — Tiếp nhận phiên chat với Pessimistic Lock chống race condition.
     * Ném ChatSessionAlreadyAssignedException nếu phiên đã được staff khác tiếp nhận.
     */
    ChatSession assignSession(Long sessionId, Long staffId);

    ChatSessionStatusResponse getSessionStatus(Long userId, String guestId);
}
