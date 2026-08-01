package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.UpdateAttendanceRequest;
import com.tourbooking.booking.backend.model.dto.response.AttendanceResponse;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.TourAttendance;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.TourAttendanceRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.TourAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourAttendanceServiceImpl implements TourAttendanceService {

    private final TourAttendanceRepository tourAttendanceRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final BookingRepository bookingRepository;
    private final com.tourbooking.booking.backend.repository.UserNotificationRepository userNotificationRepository;
    private final com.tourbooking.booking.backend.repository.UserRepository userRepository;

    @Override
    @Transactional
    public List<AttendanceResponse> getAttendancesForSchedule(Long guideId, Long scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getGuide() == null || !schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }

        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        List<TourAttendance> existingAttendances = tourAttendanceRepository.findByScheduleId(scheduleId);
        
        // Find all bookings for this schedule that are confirmed/paid/pending
        List<Booking> activeBookings = bookingRepository.findByScheduleIdAndStatusIn(scheduleId, 
                List.of(BookingStatus.CONFIRMED, BookingStatus.PAID, BookingStatus.PENDING, BookingStatus.PENDING_CASH, BookingStatus.IN_PROGRESS));
                
        // Check if any booking is missing an attendance record
        List<Long> existingBookingIds = existingAttendances.stream()
                .map(a -> a.getBooking().getId())
                .collect(Collectors.toList());
                
        boolean hasNewRecords = false;
        for (Booking b : activeBookings) {
            if (!existingBookingIds.contains(b.getId())) {
                TourAttendance newAtt = TourAttendance.builder()
                        .schedule(schedule)
                        .booking(b)
                        .status(AttendanceStatus.PENDING)
                        .build();
                tourAttendanceRepository.save(newAtt);
                existingAttendances.add(newAtt);
                hasNewRecords = true;
            }
        }

        return existingAttendances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttendanceResponse updateAttendance(Long guideId, Long scheduleId, Long attendanceId, UpdateAttendanceRequest request) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getGuide() == null || !schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }
        
        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        TourAttendance attendance = tourAttendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        if (!attendance.getSchedule().getId().equals(scheduleId)) {
            throw new RuntimeException("Attendance does not belong to this schedule");
        }
        
        if (request.getStatus() == AttendanceStatus.ABSENT) {
            // Check departure time + 15 mins
            LocalDateTime departure = schedule.getDepartureDateTime();
            if (departure != null) {
                if (LocalDateTime.now().isBefore(departure.plusMinutes(15))) {
                    throw new RuntimeException("Chỉ được đánh vắng sau 15 phút kể từ giờ khởi hành");
                }
            }
        }

        attendance.setStatus(request.getStatus());
        attendance.setMarkedAt(LocalDateTime.now());

        tourAttendanceRepository.save(attendance);

        // ── No-show alert for Admin/Staff when ABSENT is marked ────────────────
        if (request.getStatus() == AttendanceStatus.ABSENT) {
            String tourName = schedule.getTour() != null ? schedule.getTour().getTourName() : "N/A";
            String guideName = schedule.getGuide() != null ? schedule.getGuide().getFullName() : "N/A";
            String customerName = attendance.getBooking() != null && attendance.getBooking().getUser() != null
                    ? attendance.getBooking().getUser().getFullName() : "Khách hàng";
            String msg = String.format("⚠️ Khách hàng vắng mặt: %s | Tour: %s (Lịch #%d) | Khởi hành: %s | Hướng dẫn viên: %s",
                    customerName, tourName, schedule.getId(),
                    schedule.getStartDate() != null ? schedule.getStartDate().toString() : "N/A",
                    guideName);

            List<com.tourbooking.booking.backend.model.entity.User> adminsAndStaff = new java.util.ArrayList<>(userRepository.findByRole(
                    com.tourbooking.booking.backend.model.entity.enums.UserRole.ADMIN));
            adminsAndStaff.addAll(userRepository.findByRole(
                    com.tourbooking.booking.backend.model.entity.enums.UserRole.STAFF));

            for (com.tourbooking.booking.backend.model.entity.User admin : adminsAndStaff) {
                com.tourbooking.booking.backend.model.entity.UserNotification notif = new com.tourbooking.booking.backend.model.entity.UserNotification();
                notif.setUser(admin);
                notif.setTitle("Vắng mặt: " + customerName);
                notif.setMessage(msg);
                notif.setType("NO_SHOW_ALERT");
                notif.setLink("/pages/staff/schedules.html?scheduleId=" + schedule.getId());
                userNotificationRepository.save(notif);
            }
            log.info("[ATTENDANCE] No-show alert sent to {} admin/staff for schedule #{} customer {}",
                    adminsAndStaff.size(), schedule.getId(), customerName);
        }

        return mapToResponse(attendance);
    }

    private AttendanceResponse mapToResponse(TourAttendance a) {
        String name = "Unknown";
        String phone = "N/A";
        String email = "N/A";
        if (a.getBooking() != null && a.getBooking().getUser() != null) {
            com.tourbooking.booking.backend.model.entity.User u = a.getBooking().getUser();
            name = u.getFullName() != null ? u.getFullName() : u.getEmail();
            phone = u.getPhoneNumber() != null ? u.getPhoneNumber() : "N/A";
            email = u.getEmail() != null ? u.getEmail() : "N/A";
        }

        return AttendanceResponse.builder()
                .id(a.getId())
                .scheduleId(a.getSchedule().getId())
                .bookingId(a.getBooking() != null ? a.getBooking().getId() : null)
                .customerName(name)
                .customerPhone(phone)
                .customerEmail(email)
                .status(a.getStatus())
                .markedAt(a.getMarkedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendancesForAdmin(Long scheduleId) {
        List<TourAttendance> list = tourAttendanceRepository.findByScheduleId(scheduleId);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourAttendance> getAttendancesByStatus(com.tourbooking.booking.backend.model.entity.enums.AttendanceStatus status) {
        return tourAttendanceRepository.findByStatus(status);
    }
}
