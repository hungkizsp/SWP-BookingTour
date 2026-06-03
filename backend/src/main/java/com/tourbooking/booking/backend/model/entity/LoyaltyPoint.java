package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
public class LoyaltyPoint extends Base {

    @OneToOne
    private User user;

    private Integer points;
}