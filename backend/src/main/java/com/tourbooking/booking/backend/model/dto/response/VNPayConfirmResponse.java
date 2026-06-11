package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VNPayConfirmResponse {

    private boolean success;
    private String message;
    private Long bookingId;
    private String transactionRef;
    private String responseCode;
    private PaymentResponse payment;
}
