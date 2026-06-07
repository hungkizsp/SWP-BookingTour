package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerRequest {

    @NotBlank(message = "Họ tên hành khách không được để trống")
    private String fullName;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    private String idNumber;

    /** "ADULT" hoặc "CHILD" */
    @NotBlank(message = "Loại hành khách không được để trống")
    private String passengerType;
}
