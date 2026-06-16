package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response after requesting a refund
 * Used in UC21: Request Refund
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestResponse {
    private boolean success;
    private String message;
    private String refundReference;
    private BigDecimal refundAmount;
    private String refundStatus;
    private Integer expectedProcessingDays;
    private LocalDateTime requestedAt;
}
