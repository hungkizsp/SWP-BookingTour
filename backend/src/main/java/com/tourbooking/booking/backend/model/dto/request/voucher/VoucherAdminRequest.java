package com.tourbooking.booking.backend.model.dto.request.voucher;

import com.tourbooking.booking.backend.model.entity.enums.DiscountType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherAdminRequest {
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal minimumBookingAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Long applicableTourId;
}
