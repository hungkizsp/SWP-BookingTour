package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "CityID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class City extends Base {

    @Column(name = "CityName", length = 100, nullable = false, unique = true)
    private String cityName;

    @Column(name = "CenterLatitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal centerLatitude;

    @Column(name = "CenterLongitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal centerLongitude;
}

