package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class RefundRequest {
    private Long bookingId;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String reason;
}
