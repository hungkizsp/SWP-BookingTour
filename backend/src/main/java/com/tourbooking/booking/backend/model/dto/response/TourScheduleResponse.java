package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourScheduleResponse {
    private Long id;
    private Long tourId;
    private String tourName;
    private Long guideId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime departureTime;
    private LocalTime returnTime;
    private LocalDateTime bookingDeadline;
    private Integer availableSlots;
    private Integer maxSlots;
    private String status;
    private String currentProgress;
    private String reportContent;
    private LocalDateTime reportSubmittedAt;
    private List<String> imageUrls;
    private List<ProgressLogResponse> progressLogs;
}
