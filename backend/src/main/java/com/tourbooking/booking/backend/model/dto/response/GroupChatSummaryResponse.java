package com.tourbooking.booking.backend.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupChatSummaryResponse {
    private Long scheduleId;
    private String tourName;
    private String tourImage;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Long guideId;
    private String guideName;
    private String scheduleStatus;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}
