package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class RefundRequest {
    private Long bookingId;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String reason;
    private String refundInfo; // sometimes passed in history.html
    private boolean isOperatorInitiated; // true if customer is refunding due to tour suspension
}
