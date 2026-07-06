package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyRedeemResponse {
    private boolean valid;
    private BigDecimal discountAmount;
    private int remainingPoints;
    private BigDecimal finalTotal;
    private String message;
}
