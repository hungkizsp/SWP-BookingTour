package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.AiChatRequest;
import com.tourbooking.booking.backend.model.dto.response.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}

