package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatResponse {
    private String reply;
}

