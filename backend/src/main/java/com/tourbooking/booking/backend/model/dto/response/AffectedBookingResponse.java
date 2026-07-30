package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectedBookingResponse {
    private Long bookingId;
    private String bookingCode;
    private String customerName;
    private String contactEmail;
    private String contactPhone;
    private String tourName;
    private LocalDate startDate;
    private Integer numberOfPeople;
    private BigDecimal totalPrice;
    private BookingStatus status;
}
