package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "DiscountID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Discount extends Base {

    @Column(name = "Code", unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "DiscountType", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "Value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "CurrentUsage")
    private Integer currentUsage = 0;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "MinimumBookingAmount", precision = 10, scale = 2)
    private BigDecimal minimumBookingAmount;
}
