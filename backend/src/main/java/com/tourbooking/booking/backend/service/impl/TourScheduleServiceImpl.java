package com.tourbooking.booking.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.BadRequestException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.SuspendTourRequest;
import com.tourbooking.booking.backend.model.dto.response.SuspendPreviewResponse;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.TourScheduleService;
import com.tourbooking.booking.backend.service.TourChatGroupService;
import com.tourbooking.booking.backend.util.ActiveBookingStatuses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.RefundRequestRepository;
import com.tourbooking.booking.backend.service.MailService;
import java.time.LocalDate;
import java.util.List;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.RefundStatus;
import com.tourbooking.booking.backend.model.entity.RefundRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final com.tourbooking.booking.backend.repository.TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final MailService mailService;
    private final TourChatGroupService tourChatGroupService;

    @Override
    @Transactional
    public void cancelTourSchedule(Long scheduleId) {
        // Step 1: Update Schedule Status
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        
        if (schedule.getStatus() == TourStatus.CANCELLED || schedule.getStatus() == TourStatus.CANCELLED_BY_OPERATOR) {
            throw new IllegalStateException("Lịch trình này đã bị hủy trước đó.");
        }
        schedule.setStatus(TourStatus.CANCELLED);
        tourScheduleRepository.save(schedule);

        try {
            tourChatGroupService.closeGroup(scheduleId);
        } catch (Exception e) {
            log.error("Failed to close chat group for schedule {}", scheduleId, e);
        }

        // Step 2: Fetch all active bookings linked to this schedule
        List<com.tourbooking.booking.backend.model.entity.Booking> activeBookings = bookingRepository.findByScheduleIdAndStatusIn(
            scheduleId, List.of(BookingStatus.CONFIRMED, BookingStatus.PAID, BookingStatus.PENDING, BookingStatus.PENDING_CASH)
        );

        // Step 3: Loop through bookings for automated 100% refund & cancellation
        for (com.tourbooking.booking.backend.model.entity.Booking booking : activeBookings) {
            BookingStatus originalStatus = booking.getStatus();
            // Update booking status
            booking.setStatus(BookingStatus.COMPANY_CANCELED); 
            
            // Only refund if they actually paid (CONFIRMED or PAID)
            if (originalStatus == BookingStatus.CONFIRMED || originalStatus == BookingStatus.PAID) {
                // Trigger 100% Refund Logic
                RefundRequest refund = new RefundRequest();
                refund.setBooking(booking);
                refund.setAmount(booking.getTotalPrice());
                refund.setReason("Công ty hủy lịch trình");
                refund.setStatus(RefundStatus.APPROVED);
                refund.setOriginalBookingStatus(originalStatus);
                refund.setProcessedAt(java.time.LocalDateTime.now());
                refund.setStaffNote("Hoàn tiền 100% do hủy lịch trình");
                refundRequestRepository.save(refund);
            }
            
            // Queue/Send Notification to Customer
            try {
                if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                    mailService.sendOperatorCancellationRefundEmail(
                        booking.getUser().getEmail(), 
                        booking.getUser().getFullName(),
                        booking.getId(),
                        booking.getTotalPrice()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send cancellation email to user {}", booking.getUser().getEmail(), e);
            }
        }
        bookingRepository.saveAll(activeBookings);

        if (schedule.getGuide() != null) {
            schedule.setGuide(null);
            tourScheduleRepository.save(schedule);
            log.info("[CANCEL-SCHEDULE] Released guide from schedule #{} after full cancellation.", scheduleId);
        }
    }

    @Override
    @Transactional
    public void releaseGuideIfNoActiveBookings(Long scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getGuide() == null) {
            return;
        }

        long activeCount = bookingRepository.countByScheduleIdAndStatusIn(scheduleId, ActiveBookingStatuses.STATUSES);
        if (activeCount == 0) {
            Long guideId = schedule.getGuide().getId();
            schedule.setGuide(null);
            tourScheduleRepository.save(schedule);
            log.info("[RELEASE-GUIDE] Released guide #{} from schedule #{} (0 active bookings).",
                    guideId, scheduleId);
        }
    }


    /**
     * Deduct slots atomically using a Pessimistic Write Lock.
     * Automatically transitions the schedule to SOLD_OUT if no slots remain.
     * Called WITHIN the same @Transactional block as createBooking.
     */
    @Override
    @Transactional
    public TourSchedule deductAvailableSlots(Long scheduleId, int slotsToDeduct) {
        if (slotsToDeduct <= 0) {
            throw new BadRequestException("Số chỗ cần trừ phải lớn hơn 0.");
        }

        // Pessimistic Write Lock — prevents concurrent overwrites on MS SQL Server
        TourSchedule schedule = tourScheduleRepository.findByIdWithLock(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Integer available = schedule.getAvailableSlots();
        if (available == null || available <= 0) {
            // Mark SOLD_OUT proactively
            schedule.setStatus(TourStatus.SOLD_OUT);
            tourScheduleRepository.save(schedule);
            throw new AppException(ErrorCode.SCHEDULE_SOLD_OUT);
        }

        if (available < slotsToDeduct) {
            throw new AppException(ErrorCode.INSUFFICIENT_SLOTS);
        }

        int remaining = available - slotsToDeduct;
        schedule.setAvailableSlots(remaining);

        // Auto-transition to SOLD_OUT when slots reach 0
        if (remaining == 0 && schedule.getStatus() == TourStatus.OPEN) {
            schedule.setStatus(TourStatus.SOLD_OUT);
            log.info("[SLOT] Schedule #{} transitioned to SOLD_OUT after deducting {} slots.",
                    scheduleId, slotsToDeduct);
        }

        return tourScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void releaseAvailableSlots(Long scheduleId, int slotsToRelease) {
        if (slotsToRelease <= 0) {
            return;
        }

        tourScheduleRepository.findByIdWithLock(scheduleId).ifPresent(schedule -> {
            int current = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
            int newSlots = current + slotsToRelease;
            schedule.setAvailableSlots(newSlots);

            // If schedule was SOLD_OUT and slots are restored, revert to OPEN
            if (schedule.getStatus() == TourStatus.SOLD_OUT && newSlots > 0) {
                schedule.setStatus(TourStatus.OPEN);
                log.info("[SLOT] Schedule #{} reverted from SOLD_OUT to OPEN (released {} slots).",
                        scheduleId, slotsToRelease);
            }

            tourScheduleRepository.save(schedule);
        });
    }

    @Override
    @Transactional
    public List<TourSchedule> bulkCreateSchedules(
            com.tourbooking.booking.backend.model.dto.request.TourScheduleBulkRequest request) {
        if (request.getRangeStartDate() == null || request.getRangeEndDate() == null) {
            throw new BadRequestException("Range start and end dates are required");
        }
        if (request.getRangeStartDate().isAfter(request.getRangeEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(request.getRangeStartDate(),
                request.getRangeEndDate());
        if (daysBetween > 365) {
            throw new BadRequestException("Khoảng thời gian lặp lại không được vượt quá 1 năm");
        }

        com.tourbooking.booking.backend.model.entity.Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        List<TourSchedule> schedulesToSave = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate current = request.getRangeStartDate();

        int durationDays = tour.getDuration() != null && tour.getDuration() > 0 ? tour.getDuration() - 1 : 0;

        while (!current.isAfter(request.getRangeEndDate())) {
            if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty()
                    || request.getDaysOfWeek().contains(current.getDayOfWeek())) {
                    
                if (current.equals(today) && request.getDepartureTime() != null && request.getDepartureTime().isBefore(java.time.LocalTime.now())) {
                    throw new IllegalArgumentException("Không thể lặp lịch vào giờ đã qua của ngày hôm nay!");
                }

                TourSchedule schedule = new TourSchedule();
                schedule.setTour(tour);
                schedule.setStartDate(current);
                schedule.setEndDate(current.plusDays(durationDays));
                schedule.setDepartureTime(request.getDepartureTime());
                schedule.setMaxSlots(request.getMaxSlots());
                schedule.setAvailableSlots(request.getMaxSlots());

                // ── Auto-suspend if this date falls in an active suspension window ──
                List<TourSchedule> activeSuspensions = tourScheduleRepository.findActiveSuspensionCoveringDate(
                        tour.getId(), current);
                if (!activeSuspensions.isEmpty()) {
                    TourSchedule ref = activeSuspensions.get(0);
                    schedule.setStatus(TourStatus.SUSPENDED);
                    schedule.setSuspensionReasonType(ref.getSuspensionReasonType());
                    schedule.setSuspensionReason(ref.getSuspensionReason());
                    schedule.setSuspendedFrom(ref.getSuspendedFrom());
                    schedule.setSuspendedUntil(ref.getSuspendedUntil());
                    log.info("[BULK-CREATE] Schedule on {} auto-suspended (covers active suspension window of tour #{}).",
                            current, tour.getId());
                } else {
                    schedule.setStatus(TourStatus.OPEN);
                }

                schedulesToSave.add(schedule);
            }
            current = current.plusDays(1);
        }

        return tourScheduleRepository.saveAll(schedulesToSave);
    }

    // ── New date-range suspension methods ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SuspendPreviewResponse> previewSuspension(Long tourId, LocalDate from, LocalDate until) {
        List<TourSchedule> schedules = tourScheduleRepository.findByTourIdAndDateRange(tourId, from, until);

        return schedules.stream().map(s -> {
            long bookingCount = bookingRepository.countByScheduleIdAndStatusIn(
                    s.getId(),
                    List.of(BookingStatus.CONFIRMED, BookingStatus.PAID,
                            BookingStatus.PENDING, BookingStatus.PENDING_CASH));
            return SuspendPreviewResponse.builder()
                    .scheduleId(s.getId())
                    .startDate(s.getStartDate())
                    .departureTime(s.getDepartureTime() != null ? s.getDepartureTime().toString() : null)
                    .affectedBookingCount((int) bookingCount)
                    .currentStatus(s.getStatus() != null ? s.getStatus().name() : "UNKNOWN")
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public void suspendTourByDateRange(Long tourId, SuspendTourRequest request) {
        com.tourbooking.booking.backend.model.entity.Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        // Find all schedules in range
        List<TourSchedule> inRange = tourScheduleRepository.findByTourIdAndDateRange(
                tourId, request.getSuspendedFrom(), request.getSuspendedUntil());

        if (inRange.isEmpty()) {
            throw new BadRequestException("Không có lịch trình nào trong khoảng ngày đã chọn.");
        }

        // Filter to only those included by admin (deselected ones are excluded)
        List<TourSchedule> toSuspend;
        if (request.getIncludedScheduleIds() != null && !request.getIncludedScheduleIds().isEmpty()) {
            java.util.Set<Long> included = new java.util.HashSet<>(request.getIncludedScheduleIds());
            toSuspend = inRange.stream()
                    .filter(s -> included.contains(s.getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            toSuspend = inRange;
        }

        int totalAffectedBookings = 0;
        for (TourSchedule schedule : toSuspend) {
            if (schedule.getStatus() == TourStatus.CANCELLED
                    || schedule.getStatus() == TourStatus.CANCELLED_BY_OPERATOR
                    || schedule.getStatus() == TourStatus.COMPLETED) {
                continue; // Skip irreversible statuses
            }

            schedule.setStatus(TourStatus.SUSPENDED);
            schedule.setSuspensionReasonType(request.getSuspensionReasonType());
            schedule.setSuspensionReason(request.getSuspensionReason());
            schedule.setSuspendedFrom(request.getSuspendedFrom());
            schedule.setSuspendedUntil(request.getSuspendedUntil());
            
            // Notify Guide if assigned
            if (schedule.getGuide() != null) {
                mailService.sendTourSuspendedEmailToGuide(
                        schedule.getGuide().getEmail(),
                        schedule.getGuide().getFullName(),
                        tour.getTourName(),
                        request.getSuspensionReason(),
                        schedule.getStartDate()
                );
            }

            // Mark active bookings PENDING_CUSTOMER_ACTION
            List<com.tourbooking.booking.backend.model.entity.Booking> affected = bookingRepository
                    .findByScheduleIdAndStatusIn(
                            schedule.getId(),
                            List.of(BookingStatus.CONFIRMED, BookingStatus.PAID,
                                    BookingStatus.PENDING, BookingStatus.PENDING_CASH));
            for (com.tourbooking.booking.backend.model.entity.Booking booking : affected) {
                booking.setSuspensionActionStatus(
                        com.tourbooking.booking.backend.model.entity.enums.SuspensionActionStatus.PENDING_CUSTOMER_ACTION);
            }
            bookingRepository.saveAll(affected);
            totalAffectedBookings += affected.size();
        }

        tourScheduleRepository.saveAll(toSuspend);
        log.info("[SUSPEND-RANGE] Tour #{} – suspended {} schedules ({}->{}) | Reason: {} | AffectedBookings: {}",
                tourId, toSuspend.size(), request.getSuspendedFrom(), request.getSuspendedUntil(),
                request.getSuspensionReason(), totalAffectedBookings);
    }

    @Override
    @Transactional
    public void resumeTour(Long tourId) {
        tourRepository.findById(tourId).orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        List<TourSchedule> suspended = tourScheduleRepository.findSuspendedByTourId(tourId);
        if (suspended.isEmpty()) {
            throw new IllegalStateException("Tour này không có lịch trình nào đang bị tạm ngưng.");
        }

        for (TourSchedule schedule : suspended) {
            schedule.setStatus(TourStatus.OPEN);
            schedule.setSuspensionReason(null);
            schedule.setSuspensionReasonType(null);
            schedule.setSuspendedFrom(null);
            schedule.setSuspendedUntil(null);
        }
        tourScheduleRepository.saveAll(suspended);

        log.info("[RESUME-TOUR] Tour #{} – resumed {} suspended schedules.", tourId, suspended.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.tourbooking.booking.backend.model.entity.Booking> getAffectedBookings(Long scheduleId) {
        return bookingRepository.findByScheduleIdAndStatusIn(scheduleId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PAID, BookingStatus.PENDING, BookingStatus.PENDING_CASH));
    }
}
