package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourChatGroupResponse {
    private Long id;
    private Long scheduleId;
    private String tourName;
    private LocalDate startDate;
    private int memberCount;
    private Boolean isActive;
}
