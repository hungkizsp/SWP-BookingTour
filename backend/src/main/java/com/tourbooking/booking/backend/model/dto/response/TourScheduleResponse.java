package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

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
    private Integer availableSlots;
    private String status;
    private String currentProgress;
    private String reportContent;
    private java.time.LocalDateTime reportSubmittedAt;
    private List<String> imageUrls;
    private List<ProgressLogResponse> progressLogs;
}
