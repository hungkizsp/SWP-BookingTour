package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDateTime;

import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatNotification {
    private Long sessionId;
    private Long userId;
    private String guestId;
    private String customerLabel;
    private String snippet;
    private ChatSessionStatus status;
    private LocalDateTime timestamp;
    private long waitingCount;
}
