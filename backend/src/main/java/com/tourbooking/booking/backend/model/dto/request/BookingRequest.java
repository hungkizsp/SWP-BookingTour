package com.tourbooking.booking.backend.model.dto.request;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person")
    private Integer numberOfPeople;

    // UC15 (optional)
    private String voucherCode;

    // Used by update endpoints (optional)
    private BigDecimal totalPrice;
    private String discountCode;
    private BookingStatus status;
}
