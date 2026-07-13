package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserNotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
}
