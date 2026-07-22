package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TourScheduleRequest {
    private Long id;
    @jakarta.validation.constraints.NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;
    
    @jakarta.validation.constraints.NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;
    
    @jakarta.validation.constraints.NotNull(message = "Giờ khởi hành không được để trống")
    private LocalTime departureTime;
    
    private LocalTime returnTime;
    private Integer availableSlots;
    
    @jakarta.validation.constraints.NotNull(message = "Số lượng chỗ không được để trống")
    @jakarta.validation.constraints.Positive(message = "Số lượng chỗ phải lớn hơn 0")
    @jakarta.validation.constraints.Max(value = 500, message = "Số lượng chỗ không được vượt quá 500")
    private Integer maxSlots;
    /**
     * Optional: explicit booking deadline. If null, defaults to departure datetime on the backend.
     */
    private LocalDateTime bookingDeadline;
}
