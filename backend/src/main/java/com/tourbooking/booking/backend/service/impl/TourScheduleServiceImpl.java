package com.tourbooking.booking.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.BadRequestException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.TourScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final com.tourbooking.booking.backend.repository.TourRepository tourRepository;

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
    public java.util.List<TourSchedule> bulkCreateSchedules(com.tourbooking.booking.backend.model.dto.request.TourScheduleBulkRequest request) {
        if (request.getRangeStartDate() == null || request.getRangeEndDate() == null) {
            throw new BadRequestException("Khoảng thời gian áp dụng không được để trống (Range start and end dates are required).");
        }
        
        java.time.LocalDate today = java.time.LocalDate.now();
        if (request.getRangeStartDate().isBefore(today) || request.getRangeEndDate().isBefore(today)) {
            throw new BadRequestException("Ngày bắt đầu và kết thúc không được nằm trong quá khứ (Dates cannot be in the past).");
        }
        
        if (request.getRangeStartDate().isAfter(request.getRangeEndDate())) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc (Start date cannot be after end date).");
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(request.getRangeStartDate(), request.getRangeEndDate());
        if (daysBetween > 365) {
            throw new BadRequestException("Khoảng thời gian lặp lại không được vượt quá 1 năm (Duration cannot exceed 365 days).");
        }
        
        if (daysBetween > 0 && (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty())) {
            throw new BadRequestException("Vui lòng chọn ít nhất một thứ trong tuần để lặp lại (Please select at least one day of the week).");
        }

        com.tourbooking.booking.backend.model.entity.Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        java.util.List<TourSchedule> schedulesToSave = new java.util.ArrayList<>();
        java.time.LocalDate current = request.getRangeStartDate();

        int durationDays = tour.getDuration() != null && tour.getDuration() > 0 ? tour.getDuration() - 1 : 0;

        while (!current.isAfter(request.getRangeEndDate())) {
            if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty() || request.getDaysOfWeek().contains(current.getDayOfWeek())) {
                TourSchedule schedule = new TourSchedule();
                schedule.setTour(tour);
                schedule.setStartDate(current);
                schedule.setEndDate(current.plusDays(durationDays));
                schedule.setDepartureTime(request.getDepartureTime());
                schedule.setMaxSlots(request.getMaxSlots());
                schedule.setAvailableSlots(request.getMaxSlots());
                schedule.setStatus(TourStatus.OPEN);
                
                schedulesToSave.add(schedule);
            }
            current = current.plusDays(1);
        }
        
        return tourScheduleRepository.saveAll(schedulesToSave);
    }
}
