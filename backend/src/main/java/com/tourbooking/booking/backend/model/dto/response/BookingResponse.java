package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long scheduleId;
    private Long tourId;
    private String tourName;
    private LocalDateTime bookingDate;
    private java.time.LocalDate departureDate;
    private java.util.List<PassengerResponse> passengers;
    private Integer numberOfPeople;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private String discountCode;
    private BookingStatus status;
    private String refundReason;
    private String refundStatus;
    private BigDecimal refundAmount;
    
    // Additional fields for detail page
    private java.time.LocalDate returnDate;
    private String guideFullName;
    private String guidePhone;
    private String guideAvatar;
    private String guideBio;
    private Integer guideExperience;
    private String tourItinerary;
}
