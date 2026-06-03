package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDateTime;

import com.tourbooking.booking.backend.model.entity.enums.ChatEscalationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatEscalationResponse {
    private Long id;
    private Long userId;
    private String guestId;
    private ChatEscalationStatus status;
    private String requestNote;
    private String meetingPreference;
    private Long assignedStaffId;
    private String assignedStaffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
