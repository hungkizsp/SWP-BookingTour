package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DiscountPolicies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "PolicyID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class DiscountPolicy extends Base {

    @Column(name = "PassengerType", nullable = false, unique = true, length = 50)
    private String passengerType;

    @Column(name = "Rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;
}
