package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed booking information response
 * Used in UC19: View Booking Detail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {
    // Booking basic info
    private Long bookingId;
    private String bookingReference;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime bookingDate;
    
    // Tour information
    private TourInfo tourInfo;
    
    // Customer information
    private CustomerInfo customerInfo;
    
    // Payment information
    private PaymentInfo paymentInfo;
    
    // Status history
    private List<StatusHistoryItem> statusHistory;
    
    // Cancellation info (nullable)
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    
    // Refund info (nullable)
    private RefundInfo refundInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TourInfo {
        private Long tourId;
        private String tourName;
        private String destination;
        private String description;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private Integer duration; // in days
        private Integer numberOfParticipants;
        private List<String> includedServices;
        private String imageUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private Long customerId;
        private String fullName;
        private String email;
        private String phone;
        private Integer numberOfParticipants;
        private List<PassengerResponse> passengers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String paymentStatus;
        private String transactionReference;
        private String paymentMethod;
        private LocalDateTime paymentDate;
        private BigDecimal subtotal;
        private BigDecimal serviceFee;
        private BigDecimal tax;
        private BigDecimal discount;
        private BigDecimal totalAmount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryItem {
        private BookingStatus status;
        private String description;
        private LocalDateTime timestamp;
        private boolean isCurrent;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundInfo {
        private String refundStatus;
        private BigDecimal refundAmount;
        private String reason;
        private LocalDateTime requestedAt;
    }
}
