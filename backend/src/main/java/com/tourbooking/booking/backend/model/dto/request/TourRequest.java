package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

import java.util.List;

@Data
public class TourRequest {
    @jakarta.validation.constraints.NotBlank(message = "Tên tour không được để trống")
    @jakarta.validation.constraints.Size(max = 255, message = "Tên tour vượt quá độ dài tối đa (255 ký tự)")
    private String tourName;
    
    private String description;
    
    @jakarta.validation.constraints.NotNull(message = "Giá tour không được để trống")
    @jakarta.validation.constraints.Positive(message = "Giá tour phải lớn hơn 0")
    @jakarta.validation.constraints.Max(value = 99999999, message = "Giá tối đa cho phép là 99,999,999 VNĐ")
    private BigDecimal price;
    
    @jakarta.validation.constraints.NotNull(message = "Số ngày đi không được để trống")
    @jakarta.validation.constraints.Positive(message = "Số ngày đi phải lớn hơn 0")
    @jakarta.validation.constraints.Max(value = 60, message = "Số ngày tối đa là 60")
    private Integer duration;
    
    private String itinerary;
    
    @jakarta.validation.constraints.NotBlank(message = "Vui lòng nhập Điểm khởi hành")
    private String startLocation;
    
    @jakarta.validation.constraints.NotBlank(message = "Vui lòng nhập Điểm đến")
    private String endLocation;
    
    @jakarta.validation.constraints.NotBlank(message = "Vui lòng nhập Phương tiện di chuyển")
    private String transportType;
    private Long categoryId;
    private List<String> imageUrls;
    private List<String> highlights;
    @jakarta.validation.Valid
    private List<TourScheduleRequest> schedules;
    @jakarta.validation.Valid
    private List<TourItineraryDayRequest> itineraryDays;
    private String externalId;
}
