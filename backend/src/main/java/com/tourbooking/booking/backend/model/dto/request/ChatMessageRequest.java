package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long userId; // optional for guest
    private String guestId; // optional if userId is provided

    @NotBlank
    private String senderType; // GUEST / STAFF / AI

    @NotBlank
    private String message;
}

