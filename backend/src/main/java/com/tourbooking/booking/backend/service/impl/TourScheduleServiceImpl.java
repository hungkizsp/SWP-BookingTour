package com.tourbooking.booking.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.BadRequestException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.TourScheduleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;

    @Override
    @Transactional
    public TourSchedule deductAvailableSlots(Long scheduleId, int slotsToDeduct) {
        if (slotsToDeduct <= 0) {
            throw new BadRequestException("Số chỗ cần trừ phải lớn hơn 0.");
        }

        TourSchedule schedule = tourScheduleRepository.findByIdWithLock(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        Integer available = schedule.getAvailableSlots();
        if (available == null || available < slotsToDeduct) {
            throw new BadRequestException("Tour không còn đủ chỗ trống cho số hành khách này.");
        }

        schedule.setAvailableSlots(available - slotsToDeduct);
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
            schedule.setAvailableSlots(current + slotsToRelease);
            tourScheduleRepository.save(schedule);
        });
    }
}
