package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
public class Invoice extends Base {

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private String invoiceNumber;
}