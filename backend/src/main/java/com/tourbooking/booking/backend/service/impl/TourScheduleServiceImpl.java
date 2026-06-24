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
}
