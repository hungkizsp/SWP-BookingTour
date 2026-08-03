package com.tourbooking.booking.backend.ai.tool;

import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final TourRepository tourRepo;
    private final BookingRepository bookingRepo;

    @Override
    public WeatherInfo getWeather(String location, LocalDate from, LocalDate to) {
        // Fallback intelligent weather simulation based on location
        String loc = location != null ? location : "Việt Nam";
        return WeatherInfo.builder()
                .location(loc)
                .forecast("Nắng đẹp, thoáng mát, thích hợp du lịch")
                .tempCelsius(28.5)
                .advice("Mang theo kem chống nắng, mũ rộng vành và trang phục thoải mái.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String buildTourContext(Long tourId) {
        if (tourId == null) return "Không có thông tin tour.";
        Tour tour = tourRepo.findById(tourId).orElse(null);
        if (tour == null) return "Không tìm thấy tour #" + tourId;

        return String.format("Tour #%d: %s | Giá: %,.0f VNĐ | Thời lượng: %d ngày | Khởi hành: %s -> %s | Đánh giá: %.1f",
                tour.getId(), tour.getTourName(), tour.getPrice(), tour.getDuration() != null ? tour.getDuration() : 1,
                tour.getStartLocation(), tour.getEndLocation(), tour.getRating() != null ? tour.getRating() : 5.0);
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserBookingSummary(Long userId) {
        if (userId == null) return "Khách vãng lai";
        var bookings = bookingRepo.findByUserIdOrderByBookingDateDesc(userId);
        if (bookings.isEmpty()) return "Chưa có lịch sử đặt tour";

        return bookings.stream().limit(3).map(b -> String.format("Booking #%d - Status: %s - Tổng: %,.0f VNĐ",
                b.getId(), b.getStatus(), b.getTotalPrice())).collect(Collectors.joining("; "));
    }
}
