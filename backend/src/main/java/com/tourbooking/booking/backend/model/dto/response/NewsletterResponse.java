package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NewsletterResponse {
    private Long id;
    private String email;
    private LocalDateTime subscribedAt;
    private boolean subscribed;
}
