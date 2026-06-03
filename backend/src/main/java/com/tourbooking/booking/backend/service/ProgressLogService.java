package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.ProgressLogResponse;
import com.tourbooking.booking.backend.model.entity.TourProgressLog;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.repository.TourProgressLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tách biệt việc load progress logs ra một transaction riêng (REQUIRES_NEW)
 * để tránh transaction chính bị đánh dấu rollback-only
 * khi bảng TourProgressLogs chưa tồn tại hoặc có lỗi SQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressLogService {

    private final TourProgressLogRepository tourProgressLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public List<ProgressLogResponse> loadProgressLogs(TourSchedule schedule) {
        try {
            return tourProgressLogRepository.findByScheduleOrderByCreatedAtDesc(schedule)
                    .stream()
                    .filter(l -> l != null)
                    .map(l -> ProgressLogResponse.builder()
                            .id(l.getId())
                            .content(l.getContent())
                            .createdAt(l.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not load progress logs for schedule {}: {}", schedule.getId(), e.getMessage());
            return new ArrayList<>();
        }
    }
}
