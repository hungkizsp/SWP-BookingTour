package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class CancelBookingRequest {
    private String reason;
}
