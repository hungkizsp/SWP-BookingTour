package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long userId;
    private String guestId;
    private String senderType;
    private String message;
    private LocalDateTime sentAt;
}

