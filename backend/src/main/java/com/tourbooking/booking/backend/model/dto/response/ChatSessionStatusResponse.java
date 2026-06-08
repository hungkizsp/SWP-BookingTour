package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionStatusResponse {
    private Long id;
    private ChatSessionStatus status;
    private String assignedStaffName;
}
