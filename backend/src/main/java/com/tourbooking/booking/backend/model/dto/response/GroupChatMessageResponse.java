package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupChatMessageResponse {
    private Long id;
    private Long scheduleId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderRole;
    private String message;
    private LocalDateTime sentAt;
}
