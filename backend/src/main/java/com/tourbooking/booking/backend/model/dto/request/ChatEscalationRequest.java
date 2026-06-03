package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class ChatEscalationRequest {
    private Long userId;
    private String guestId;
    private String requestNote;
    private String meetingPreference;
}
