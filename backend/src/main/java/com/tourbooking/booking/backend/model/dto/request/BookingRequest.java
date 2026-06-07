package com.tourbooking.booking.backend.model.dto.request;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    /** Số người lớn (>= 1) */
    @NotNull(message = "Adult count is required")
    @Min(value = 1, message = "At least 1 adult")
    private Integer adultCount;

    /** Số trẻ em (>= 0) */
    @Min(value = 0, message = "Child count cannot be negative")
    private Integer childCount = 0;

    /** Tổng số người = adultCount + childCount (computed) */
    public int getNumberOfPeople() {
        return (adultCount != null ? adultCount : 0) + (childCount != null ? childCount : 0);
    }

    /** Danh sách chi tiết hành khách */
    @Valid
    private List<PassengerRequest> passengers = new ArrayList<>();

    // UC15 (optional)
    private String voucherCode;

    // Used by update endpoints (optional)
    private BigDecimal totalPrice;
    private String discountCode;
    private BookingStatus status;
}
