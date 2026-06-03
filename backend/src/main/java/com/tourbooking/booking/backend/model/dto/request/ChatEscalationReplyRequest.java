package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatEscalationReplyRequest {
    @NotBlank
    private String message;
}
