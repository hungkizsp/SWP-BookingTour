package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerResponse {
    private Long id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String idNumber;
    private String passengerType;
}
