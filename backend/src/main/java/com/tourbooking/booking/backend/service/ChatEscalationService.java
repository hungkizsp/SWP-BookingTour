package com.tourbooking.booking.backend.service;

import java.util.List;

import com.tourbooking.booking.backend.model.dto.request.ChatEscalationReplyRequest;
import com.tourbooking.booking.backend.model.dto.request.ChatEscalationRequest;
import com.tourbooking.booking.backend.model.dto.response.ChatEscalationResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatEscalationSummaryResponse;

public interface ChatEscalationService {
    ChatEscalationResponse requestEscalation(ChatEscalationRequest request);
    List<ChatEscalationSummaryResponse> listActiveEscalations();
    ChatEscalationResponse reply(Long escalationId, Long staffId, ChatEscalationReplyRequest request);
    ChatEscalationResponse resolve(Long escalationId, Long staffId);
    ChatEscalationResponse assign(Long escalationId, Long staffId);
    ChatEscalationResponse fetchActiveEscalation(Long userId, String guestId);
    ChatEscalationResponse closeByRequester(Long userId, String guestId);
}
