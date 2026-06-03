package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class VoucherResponse {
    @com.fasterxml.jackson.annotation.JsonProperty("isValid")
    private boolean isValid;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private String message;
}
