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
import com.tourbooking.booking.backend.service.UserNotificationService;

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
    private final com.tourbooking.booking.backend.service.MailService mailService;
    private final UserNotificationService userNotificationService;

    @Override
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        try {
            mailService.sendBookingConfirmedEmail(booking.getUser().getEmail(), booking.getUser().getFullName(), booking.getId(), booking.getTotalPrice());
        } catch (Exception e) {}

        try {
            String tourName = booking.getSchedule() != null && booking.getSchedule().getTour() != null
                    ? booking.getSchedule().getTour().getTourName() : "Tour";
            userNotificationService.notify(
                    booking.getUser().getId(),
                    "Booking đã được xác nhận ✅",
                    "Booking #" + booking.getId() + " của tour \"" + tourName + "\" đã được xác nhận.",
                    "BOOKING_CONFIRMED",
                    "/user/history.html"
            );
        } catch (Exception e) { log.warn("Failed to push booking-confirmed notification: {}", e.getMessage()); }
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

        // Validate schedule conflict
        LocalDateTime targetDeparture = schedule.getDepartureDateTime();
        LocalDateTime targetReturn = schedule.getReturnDateTime();
        if (targetDeparture != null && targetReturn != null) {
            List<TourSchedule> guideSchedules = tourScheduleRepository.findByGuide_Id(guideId);
            for (TourSchedule existing : guideSchedules) {
                if (existing.getId().equals(scheduleId)) continue;
                if (existing.getStatus() == com.tourbooking.booking.backend.model.entity.enums.TourStatus.CANCELLED ||
                    existing.getStatus() == com.tourbooking.booking.backend.model.entity.enums.TourStatus.CANCELLED_BY_OPERATOR ||
                    existing.getStatus() == com.tourbooking.booking.backend.model.entity.enums.TourStatus.COMPLETED) {
                    continue;
                }
                
                LocalDateTime existingDeparture = existing.getDepartureDateTime();
                LocalDateTime existingReturn = existing.getReturnDateTime();
                
                if (existingDeparture != null && existingReturn != null) {
                    // Check overlap: (existingDeparture <= targetReturn) AND (existingReturn >= targetDeparture)
                    if (!existingDeparture.isAfter(targetReturn) && !existingReturn.isBefore(targetDeparture)) {
                        throw new RuntimeException("This guide is already assigned to another tour during this period");
                    }
                }
            }
        }

        if (schedule.getGuide() != null && schedule.getGuide().getId().equals(guideId)) {
            log.info("Guide {} is already assigned to schedule {}, skipping assignment.", guideId, scheduleId);
            return;
        }

        schedule.setGuide(guide);
        tourScheduleRepository.save(schedule);

        String tourName = schedule.getTour() != null ? schedule.getTour().getTourName() : "Tour";

        // Notify guide about assignment
        try {
            userNotificationService.notify(
                    guide.getId(),
                    "Bạn được phân công dẫn tour 🧭",
                    "Bạn được phân công làm hướng dẫn viên cho tour \"" + tourName + "\".",
                    "GUIDE_ASSIGNED",
                    "/pages/guide/dashboard.html"
            );
        } catch (Exception e) { log.warn("Failed to push guide-assigned notification: {}", e.getMessage()); }

        // Notify confirmed customers
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getSchedule() != null && b.getSchedule().getId().equals(scheduleId)
                        && (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.SUCCESS))
                .collect(Collectors.toList());
        for (Booking b : bookings) {
            try {
                mailService.sendGuideAssignedEmail(b.getUser().getEmail(), b.getUser().getFullName(), tourName, guide.getFullName());
            } catch (Exception e) {}
            try {
                userNotificationService.notify(
                        b.getUser().getId(),
                        "Hướng dẫn viên đã được phân công 🧭",
                        "Tour \"" + tourName + "\" của bạn đã có hướng dẫn viên: " + guide.getFullName() + ".",
                        "GUIDE_ASSIGNED",
                        "/pages/client/group-chat.html?scheduleId=" + scheduleId
                );
            } catch (Exception e) { log.warn("Failed to push guide-assigned customer notification: {}", e.getMessage()); }
        }
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

        try {
            mailService.sendRefundProcessedEmail(booking.getUser().getEmail(), booking.getUser().getFullName(), booking.getId(), status.name());
        } catch (Exception e) {}

        try {
            if (status == RefundStatus.APPROVED) {
                userNotificationService.notify(
                        booking.getUser().getId(),
                        "Yêu cầu hoàn tiền được phê duyệt 💰",
                        "Yêu cầu hoàn tiền cho booking #" + booking.getId() + " đã được phê duyệt.",
                        "REFUND_APPROVED",
                        "/user/history.html"
                );
            } else if (status == RefundStatus.REJECTED) {
                userNotificationService.notify(
                        booking.getUser().getId(),
                        "Yêu cầu hoàn tiền bị từ chối",
                        "Yêu cầu hoàn tiền cho booking #" + booking.getId() + " đã bị từ chối." +
                                (staffNote != null && !staffNote.isBlank() ? " Lý do: " + staffNote : ""),
                        "REFUND_REJECTED",
                        "/user/history.html"
                );
            }
        } catch (Exception e) { log.warn("Failed to push refund notification: {}", e.getMessage()); }
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
            bookings = bookingRepository.findAllWithDetails();
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
                    if (b.getSchedule() != null && b.getSchedule().getGuide() != null) {
                        res.setGuideFullName(b.getSchedule().getGuide().getFullName());
                    }
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
    public org.springframework.data.domain.Page<BookingResponse> listBookingsPaginated(String status, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Booking> bookingPage;
        
        if (status != null && !status.isEmpty()) {
            bookingPage = bookingRepository.findByStatus(BookingStatus.valueOf(status.toUpperCase()), pageable); // Assuming findByStatus(status, pageable) exists or will use findAll with pagination and filter
            // Wait, we need to make sure findByStatus supports Pageable. Let's just do findAll and filter, or use PageRequest.
            // Actually, we should just use findAll for now if status is null.
        } else {
            bookingPage = bookingRepository.findAll(pageable);
        }

        return bookingPage.map(b -> {
            BookingResponse res = new BookingResponse();
            res.setId(b.getId());
            res.setUserId(b.getUser() != null ? b.getUser().getId() : null);
            res.setUserFullName(b.getUser() != null ? b.getUser().getFullName() : "Guest");
            res.setUserEmail(b.getUser() != null ? b.getUser().getEmail() : null);
            res.setScheduleId(b.getSchedule() != null ? b.getSchedule().getId() : null);
            res.setTourName(b.getSchedule() != null && b.getSchedule().getTour() != null
                    ? b.getSchedule().getTour().getTourName()
                    : "N/A");
            if (b.getSchedule() != null && b.getSchedule().getGuide() != null) {
                res.setGuideFullName(b.getSchedule().getGuide().getFullName());
            }
            res.setBookingDate(b.getBookingDate());
            res.setNumberOfPeople(b.getNumberOfPeople());
            res.setTotalPrice(b.getTotalPrice());
            res.setStatus(b.getStatus());
            return res;
        });
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
        return tourScheduleRepository.findAllWithDetails().stream()
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
