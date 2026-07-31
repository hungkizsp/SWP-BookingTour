package com.tourbooking.booking.backend.model.dto.response;

import com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long scheduleId;
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private AttendanceStatus status;
    private LocalDateTime markedAt;
    private String lateNote;
    private Integer lateMinutes;
}
