package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LoyaltyPointResponse {
    private int totalPoints;
    private BigDecimal pointsValue;
    private List<LoyaltyTransactionDto> transactions;

    @Data
    @Builder
    public static class LoyaltyTransactionDto {
        private String transactionType;
        private int points;
        private String description;
        private String createdAt;
    }
}
