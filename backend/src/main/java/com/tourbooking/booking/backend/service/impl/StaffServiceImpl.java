package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.dto.response.RefundResponse;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.RefundRequest;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.RefundStatus;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.RefundRequestRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.StaffService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.model.dto.response.ProgressLogResponse;
import com.tourbooking.booking.backend.repository.TourProgressLogRepository;
import com.tourbooking.booking.backend.service.ProgressLogService;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final UserRepository userRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final TourProgressLogRepository tourProgressLogRepository;
    private final ProgressLogService progressLogService;

    @Override
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void assignGuide(Long scheduleId, Long guideId) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found"));

        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new RuntimeException("Guide not found"));

        if (guide.getRole() != UserRole.GUIDE) {
            throw new RuntimeException("User selected is not a GUIDE");
        }

        schedule.setGuide(guide);
        tourScheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> listRefundRequests() {
        return refundRequestRepository.findAll().stream()
                .map(req -> RefundResponse.builder()
                        .id(req.getId())
                        .bookingId(req.getBooking() != null ? req.getBooking().getId() : null)
                        .amount(req.getAmount())
                        .reason(req.getReason())
                        .status(req.getStatus().name())
                        .staffNote(req.getStaffNote())
                        .processedAt(req.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 2.2 — Nhân viên phê duyệt / từ chối yêu cầu hoàn tiền.
     * <ul>
     *   <li>APPROVE: đổi Booking → CANCELLED, hoàn trả availableSlots.</li>
     *   <li>REJECT: trả Booking về trạng thái trước đó (CONFIRMED hoặc SUCCESS).</li>
     * </ul>
     */
    @Override
    @Transactional
    public void processRefund(Long refundId, RefundStatus status, String staffNote) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund request not found"));

        Booking booking = refund.getBooking();
        if (booking == null) {
            throw new RuntimeException("Refund #" + refundId + " không liên kết với Booking nào.");
        }

        refund.setStaffNote(staffNote);
        refund.setProcessedAt(LocalDateTime.now());

        if (status == RefundStatus.APPROVED) {
            refund.setStatus(RefundStatus.APPROVED);

            // a. Cập nhật Booking → CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // b. Hoàn trả availableSlots vào TourSchedule bằng Atomic Update chống tranh chấp Lost Update
            TourSchedule schedule = booking.getSchedule();
            if (schedule != null) {
                Integer slotsToRelease = booking.getOccupiedSlots() != null
                        ? booking.getOccupiedSlots()
                        : booking.getNumberOfPeople();
                if (slotsToRelease != null && slotsToRelease > 0) {
                    tourScheduleRepository.releaseAvailableSlots(schedule.getId(), slotsToRelease);
                    log.info("[Refund] Atomic released {} slots for ScheduleID={}", slotsToRelease, schedule.getId());
                }
            }

        } else if (status == RefundStatus.REJECTED) {
            refund.setStatus(RefundStatus.REJECTED);

            // Khôi phục trạng thái booking từ dữ liệu gốc đã lưu — KHÔNG hardcode
            BookingStatus restoredStatus = refund.getOriginalBookingStatus();
            if (restoredStatus == null) {
                // Fallback an toàn nếu cột OriginalBookingStatus chưa được điền (dữ liệu cũ)
                restoredStatus = BookingStatus.CONFIRMED;
                log.warn("[Refund] RefundID={} thiếu OriginalBookingStatus → fallback CONFIRMED", refundId);
            }
            booking.setStatus(restoredStatus);
            bookingRepository.save(booking);
            log.info("[Refund] REJECT RefundID={} → Booking#{} phục hồi trạng thái: {}",
                    refundId, booking.getId(), restoredStatus);
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ: " + status);
        }

        refundRequestRepository.save(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listGuides() {
        return userRepository.findByRole(UserRole.GUIDE).stream()
                .map(u -> {
                    UserResponse res = new UserResponse();
                    res.setId(u.getId());
                    res.setFullName(u.getFullName());
                    res.setEmail(u.getEmail());
                    res.setRole(u.getRole());
                    res.setIsActive(u.getIsActive());
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> listBookings(String status) {
        List<Booking> bookings;
        if (status != null && !status.isEmpty()) {
            bookings = bookingRepository.findByStatus(BookingStatus.valueOf(status.toUpperCase()));
        } else {
            bookings = bookingRepository.findAll();
        }

        return bookings.stream()
                .map(b -> {
                    BookingResponse res = new BookingResponse();
                    res.setId(b.getId());
                    res.setUserId(b.getUser() != null ? b.getUser().getId() : null);
                    res.setUserFullName(b.getUser() != null ? b.getUser().getFullName() : "Guest");
                    res.setUserEmail(b.getUser() != null ? b.getUser().getEmail() : null);
                    res.setScheduleId(b.getSchedule() != null ? b.getSchedule().getId() : null);
                    res.setTourName(b.getSchedule() != null && b.getSchedule().getTour() != null
                            ? b.getSchedule().getTour().getTourName()
                            : "N/A");
                    res.setBookingDate(b.getBookingDate());
                    res.setNumberOfPeople(b.getNumberOfPeople());
                    res.setTotalPrice(b.getTotalPrice());
                    res.setStatus(b.getStatus());
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TourScheduleResponse getScheduleDetails(Long id) {
        System.out.println("DEBUG: Executing getScheduleDetails for ID: " + id);
        TourSchedule schedule = tourScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found with ID: " + id));
        return mapToResponse(schedule, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourScheduleResponse> listSchedules() {
        return tourScheduleRepository.findAll().stream()
                .map(s -> mapToResponse(s, false))
                .collect(Collectors.toList());
    }

    private TourScheduleResponse mapToResponse(TourSchedule schedule, boolean includeFullLogs) {
        try {
            var res = TourScheduleResponse.builder()
                    .id(schedule.getId())
                    .tourId(schedule.getTour() != null ? schedule.getTour().getId() : null)
                    .tourName(schedule.getTour() != null ? schedule.getTour().getTourName() : null)
                    .guideId(schedule.getGuide() != null ? schedule.getGuide().getId() : null)
                    .startDate(schedule.getStartDate())
                    .endDate(schedule.getEndDate())
                    .availableSlots(schedule.getAvailableSlots())
                    .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                    .currentProgress(schedule.getCurrentProgress())
                    .reportContent(schedule.getReportContent())
                    .reportSubmittedAt(schedule.getReportSubmittedAt())
                    .build();

            if (schedule.getActivityImages() != null) {
                res.setImageUrls(schedule.getActivityImages().stream()
                        .filter(img -> img != null)
                        .map(img -> img.getImageUrl())
                        .collect(Collectors.toList()));
            } else {
                res.setImageUrls(new java.util.ArrayList<>());
            }

            if (includeFullLogs) {
                res.setProgressLogs(progressLogService.loadProgressLogs(schedule));
            } else {
                res.setProgressLogs(new java.util.ArrayList<>());
            }

            return res;
        } catch (Exception e) {
            System.err.println("ERROR in mapToResponse for schedule " + schedule.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return TourScheduleResponse.builder()
                    .id(schedule.getId())
                    .tourName("Error: " + e.getMessage())
                    .build();
        }
    }
}
