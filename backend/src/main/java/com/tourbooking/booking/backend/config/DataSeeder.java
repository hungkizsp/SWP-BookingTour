package com.tourbooking.booking.backend.config;

import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// @Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TourRepository tourRepository;
    private final TourScheduleRepository tourScheduleRepository;

    @Override
    public void run(String... args) throws Exception {
        List<Tour> tours = tourRepository.findAll();
        
        if (tours.isEmpty()) {
            log.warn("Chưa có Tour nào trong database để nạp lịch trình.");
            return;
        }

        Tour targetTour = tours.get(0);
        // Kiểm tra số lượng lịch trình của tour đầu tiên
        long count = tourScheduleRepository.countByTour_Id(targetTour.getId());

        if (count < 10) {
            log.info("Đang nạp 100 lịch trình mẫu cho Tour: {}", targetTour.getTourName());
            
            for (int i = 1; i <= 100; i++) {
                TourSchedule schedule = new TourSchedule();
                schedule.setTour(targetTour);
                schedule.setAvailableSlots(20);
                // Dùng LocalDate theo cấu trúc Entity
                schedule.setStartDate(LocalDate.now().plusDays(i));
                schedule.setEndDate(LocalDate.now().plusDays(i));
                // Dùng Enum TourStatus.OPEN
                schedule.setStatus(TourStatus.OPEN);
                tourScheduleRepository.save(schedule);
            }
            log.info("Nạp xong 100 lịch trình cho '{}'!", targetTour.getTourName());
        }
    }
}
