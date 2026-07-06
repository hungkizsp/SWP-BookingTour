package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.DiscountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherAdminResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal minimumBookingAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer currentUsage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Long applicableTourId;
}
