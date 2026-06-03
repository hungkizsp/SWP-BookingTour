package com.tourbooking.booking.backend.model.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String staffNote;
    private LocalDateTime processedAt;
}
