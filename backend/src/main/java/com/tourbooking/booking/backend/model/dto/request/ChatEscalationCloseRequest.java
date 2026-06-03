package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class ChatEscalationCloseRequest {
    private Long userId;
    private String guestId;
}
