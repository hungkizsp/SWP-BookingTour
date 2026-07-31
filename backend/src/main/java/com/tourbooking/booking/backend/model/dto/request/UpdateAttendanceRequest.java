package com.tourbooking.booking.backend.model.dto.request;

import com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus;
import lombok.Data;

@Data
public class UpdateAttendanceRequest {
    private AttendanceStatus status;
    private String lateNote;
    private Integer lateMinutes;
}
