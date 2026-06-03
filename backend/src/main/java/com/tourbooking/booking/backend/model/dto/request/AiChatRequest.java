package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    private Long userId; // optional for guest

    private String guestId;

    @NotBlank
    private String message;
}
