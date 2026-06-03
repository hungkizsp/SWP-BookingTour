package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.ChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ChatMessageResponse;

import java.util.List;

public interface ChatService {
    ChatMessageResponse sendMessage(ChatMessageRequest request);

    /**
     * If userId is null: check guestId.
     */
    List<ChatMessageResponse> getConversation(Long userId, String guestId);

    void escalateToHuman(Long userId, String guestId);

    List<com.tourbooking.booking.backend.model.entity.ChatSession> getActiveSessionsForStaff();

    void closeSession(Long userId, String guestId);
}
