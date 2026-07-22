package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoyaltyRedeemRequest {
    private int pointsToRedeem;
    private BigDecimal bookingTotal;
    private Long bookingId; // for actual redeem action
}
