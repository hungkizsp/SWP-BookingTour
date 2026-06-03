package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentProcessRequest {
    private Long bookingId;
    private BigDecimal amount;
    private String paymentMethod; // VNPAY, MOMO, INSTALLMENT
    private Integer installmentMonths; // for installment
}
