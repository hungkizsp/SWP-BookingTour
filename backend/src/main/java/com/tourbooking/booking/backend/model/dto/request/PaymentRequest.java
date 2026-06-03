package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    private Long bookingId;

    // UC16: chọn phương thức
    private String paymentMethod;
    // ví dụ: VNPAY, MOMO, CASH

    // UC18: thanh toán từng phần (optional)
    private BigDecimal amount;

    // optional: mã giao dịch (nếu mock)
    private String transactionCode;
}