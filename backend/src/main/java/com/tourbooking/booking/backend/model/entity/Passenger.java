package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "Passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "PassengerID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Passenger extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", nullable = false, columnDefinition = "BIGINT")
    private Booking booking;

    @Column(name = "FullName", length = 200, nullable = false)
    private String fullName;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "IdNumber", length = 50)
    private String idNumber;

    /** "ADULT" hoặc "CHILD" */
    @Column(name = "PassengerType", length = 20, nullable = false)
    private String passengerType;
}
