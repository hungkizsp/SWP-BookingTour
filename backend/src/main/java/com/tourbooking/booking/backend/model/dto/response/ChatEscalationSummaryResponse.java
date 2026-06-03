package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDateTime;

import com.tourbooking.booking.backend.model.entity.enums.ChatEscalationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatEscalationSummaryResponse {
    private Long id;
    private Long userId;
    private String guestId;
    private String customerLabel;
    private ChatEscalationStatus status;
    private String requestNote;
    private String meetingPreference;
    private String latestMessageSnippet;
    private String assignedStaffName;
    private LocalDateTime updatedAt;
}
