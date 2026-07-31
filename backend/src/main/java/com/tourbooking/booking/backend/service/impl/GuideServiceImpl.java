package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.TourActivityImage;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.TourActivityImageRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.GuideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.tourbooking.booking.backend.model.dto.response.ProgressLogResponse;
import com.tourbooking.booking.backend.model.entity.TourProgressLog;
import com.tourbooking.booking.backend.repository.TourProgressLogRepository;
import com.tourbooking.booking.backend.service.ProgressLogService;
import com.tourbooking.booking.backend.util.ActiveBookingStatuses;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourActivityImageRepository tourActivityImageRepository;
    private final TourProgressLogRepository tourProgressLogRepository;
    private final ProgressLogService progressLogService;
    private final com.tourbooking.booking.backend.repository.BookingRepository bookingRepository;

    private static final String UPLOAD_DIR = "uploads";

    @Override
    @Transactional(readOnly = true)
    public List<TourScheduleResponse> getAssignedTours(Long guideId) {
        log.info("Fetching assigned tours for guide ID: {}", guideId);
        List<TourSchedule> schedules = tourScheduleRepository.findByGuide_Id(guideId);
        log.info("Found {} schedules for guide ID: {}", schedules.size(), guideId);
        return schedules.stream()
                .filter(s -> s.getStatus() != TourStatus.SUSPENDED)
                .filter(s -> bookingRepository.countByScheduleIdAndStatusIn(s.getId(), ActiveBookingStatuses.STATUSES) > 0)
                .map(s -> mapToResponse(s, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TourScheduleResponse getAssignedTourDetails(Long guideId, Long scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found"));

        if (!schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }
        
        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        return mapToResponse(schedule, true);
    }

    @Override
    @Transactional
    public void updateTourProgress(Long guideId, Long scheduleId, String progress) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found"));

        if (!schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }
        
        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        schedule.setCurrentProgress(progress);
        
        // Save history log
        TourProgressLog logEntry = TourProgressLog.builder()
                .schedule(schedule)
                .content(progress)
                .build();
        tourProgressLogRepository.save(logEntry);

        if (schedule.getStatus() == TourStatus.SOLD_OUT
                || schedule.getStatus() == TourStatus.OPEN
                || schedule.getStatus() == TourStatus.BOOKING_CLOSED) {
            schedule.setStatus(TourStatus.IN_PROGRESS);
        }
        tourScheduleRepository.save(schedule);

        if (schedule.getStatus() == TourStatus.IN_PROGRESS) {
            List<com.tourbooking.booking.backend.model.entity.Booking> bookings = bookingRepository.findByScheduleId(scheduleId);
            for (com.tourbooking.booking.backend.model.entity.Booking b : bookings) {
                if (b.getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.CONFIRMED) {
                    b.setStatus(com.tourbooking.booking.backend.model.entity.enums.BookingStatus.IN_PROGRESS);
                }
            }
            bookingRepository.saveAll(bookings);
        }
    }

    @Override
    @Transactional
    public void uploadTourPhotos(Long guideId, Long scheduleId, List<MultipartFile> photos) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found"));

        if (!schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }
        
        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        if (photos == null || photos.isEmpty()) return;

        try {
            Path root = Paths.get(UPLOAD_DIR);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            for (MultipartFile photo : photos) {
                if (photo.isEmpty()) continue;

                String originalName = photo.getOriginalFilename();
                String fileName = java.util.UUID.randomUUID().toString() + "_" + (originalName != null ? originalName : "image.jpg");
                Path targetPath = root.resolve(fileName);

                Files.copy(photo.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                TourActivityImage image = new TourActivityImage();
                image.setSchedule(schedule);
                image.setImageUrl("/uploads/" + fileName);
                tourActivityImageRepository.save(image);
                
                log.info("Saved photo to: {}", targetPath.toString());
            }
        } catch (Exception e) {
            log.error("Failed to upload photos: {}", e.getMessage());
            throw new RuntimeException("Could not store files. Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void submitTourReport(Long guideId, Long scheduleId, String reportContent) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Tour schedule not found"));

        if (!schedule.getGuide().getId().equals(guideId)) {
            throw new RuntimeException("You are not assigned to this tour");
        }
        
        if (schedule.getStatus() == TourStatus.SUSPENDED) {
            throw new RuntimeException("Lịch trình này đang tạm ngưng, không thể thao tác");
        }

        schedule.setReportContent(reportContent);
        schedule.setReportSubmittedAt(LocalDateTime.now());
        schedule.setStatus(TourStatus.COMPLETED);
        tourScheduleRepository.save(schedule);

        List<com.tourbooking.booking.backend.model.entity.Booking> bookings = bookingRepository.findByScheduleId(scheduleId);
        for (com.tourbooking.booking.backend.model.entity.Booking b : bookings) {
            if (b.getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.IN_PROGRESS || b.getStatus() == com.tourbooking.booking.backend.model.entity.enums.BookingStatus.CONFIRMED) {
                b.setStatus(com.tourbooking.booking.backend.model.entity.enums.BookingStatus.COMPLETED);
            }
        }
        bookingRepository.saveAll(bookings);
    }

    private TourScheduleResponse mapToResponse(TourSchedule schedule, boolean includeFullDetails) {
        try {
            TourScheduleResponse res = TourScheduleResponse.builder()
                    .id(schedule.getId())
                    .tourId(schedule.getTour() != null ? schedule.getTour().getId() : null)
                    .tourName(schedule.getTour() != null ? schedule.getTour().getTourName() : null)
                    .guideId(schedule.getGuide() != null ? schedule.getGuide().getId() : null)
                    .startDate(schedule.getStartDate())
                    .endDate(schedule.getEndDate())
                    .departureTime(schedule.getDepartureTime())
                    .returnTime(schedule.getReturnTime())
                    .bookingDeadline(schedule.getEffectiveBookingDeadline())
                    .availableSlots(schedule.getAvailableSlots())
                    .maxSlots(schedule.getMaxSlots())
                    .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                    .currentProgress(schedule.getCurrentProgress())
                    .reportContent(schedule.getReportContent())
                    .reportSubmittedAt(schedule.getReportSubmittedAt())
                    .build();

            if (includeFullDetails && schedule.getActivityImages() != null) {
                res.setImageUrls(schedule.getActivityImages().stream()
                        .filter(img -> img != null)
                        .map(TourActivityImage::getImageUrl)
                        .collect(Collectors.toList()));
            } else {
                res.setImageUrls(new java.util.ArrayList<>());
            }

            if (includeFullDetails) {
                res.setProgressLogs(progressLogService.loadProgressLogs(schedule));
            } else {
                res.setProgressLogs(new java.util.ArrayList<>());
            }

            return res;
        } catch (Exception e) {
            log.error("Fatal error mapping schedule {}: {}", schedule.getId(), e.getMessage());
            return TourScheduleResponse.builder().id(schedule.getId()).tourName("Error").build();
        }
    }
}
