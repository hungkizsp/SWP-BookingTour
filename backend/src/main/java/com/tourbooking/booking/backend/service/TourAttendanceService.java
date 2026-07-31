package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.UpdateAttendanceRequest;
import com.tourbooking.booking.backend.model.dto.response.AttendanceResponse;

import java.util.List;

public interface TourAttendanceService {
    List<AttendanceResponse> getAttendancesForSchedule(Long guideId, Long scheduleId);
    AttendanceResponse updateAttendance(Long guideId, Long scheduleId, Long attendanceId, UpdateAttendanceRequest request);
    List<AttendanceResponse> getAttendancesForAdmin(Long scheduleId);
    List<com.tourbooking.booking.backend.model.entity.TourAttendance> getAttendancesByStatus(com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus status);
}
