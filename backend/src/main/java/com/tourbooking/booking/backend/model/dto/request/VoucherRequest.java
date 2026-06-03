package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VoucherRequest {
    private String voucherCode;
    private BigDecimal currentTotal;
}
