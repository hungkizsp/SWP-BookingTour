package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service chứa tất cả các tác vụ tự động (Scheduled Jobs):
 * - UC46: Tự động cập nhật chỗ trống của TourSchedule
 * - UC47: Tự động hủy Booking chưa thanh toán quá 24h
 * - UC48: Gửi email thông báo khi hủy booking (tích hợp trong UC47)
 * - UC49: Tự động cộng điểm loyal cho booking COMPLETED (stub - chờ entity
 * LoyaltyPoint)
 * - UC50: Tạo và gửi báo cáo tháng cho ADMIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final TourRepository tourRepository;

    // ================================================================
    // UC46: Tự động cập nhật chỗ trống (AvailableSlots) cho TourSchedule
    // Chạy mỗi 5 phút
    // ================================================================
    // @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoUpdateSlots() {
        List<TourSchedule> openSchedules = tourScheduleRepository.findAllOpen();
        if (openSchedules.isEmpty())
            return;

        int updated = 0;
        for (TourSchedule schedule : openSchedules) {
            // Đếm số booking CONFIRMED trong schedule
            long confirmedCount = 0;
            if (schedule.getBookings() != null) {
                confirmedCount = schedule.getBookings().stream()
                        .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                        .mapToLong(b -> b.getNumberOfPeople() != null ? b.getNumberOfPeople() : 0)
                        .sum();
            }

            // Lấy tổng slot ban đầu (giả sử được lưu; nếu < 0 thì đặt về 0)
            // AvailableSlots hiện tại = tổng ban đầu - đã đặt CONFIRMED
            // Để đơn giản: không cho slot âm và đánh dấu FULL nếu == 0
            if (schedule.getAvailableSlots() != null && schedule.getAvailableSlots() <= 0) {
                schedule.setStatus(TourStatus.FULL);
                tourScheduleRepository.save(schedule);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("[UC46] Cập nhật {} TourSchedule sang trạng thái FULL.", updated);
        }
    }

    // ================================================================
    // UC47: Tự động hủy Booking PENDING chưa thanh toán quá 24 giờ
    // UC48: Gửi email thông báo cho khách hàng khi booking bị hủy
    // Chạy mỗi 1 giờ
    // ================================================================
    // @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void autoCancelUnpaidBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Booking> unpaidBookings = bookingRepository.findPendingUnpaidBefore(cutoff);

        if (unpaidBookings.isEmpty())
            return;

        for (Booking booking : unpaidBookings) {
            // Hủy booking
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Trả lại slot cho TourSchedule
            TourSchedule schedule = booking.getSchedule();
            if (schedule != null && booking.getNumberOfPeople() != null) {
                int currentSlots = schedule.getAvailableSlots() != null ? schedule.getAvailableSlots() : 0;
                schedule.setAvailableSlots(currentSlots + booking.getNumberOfPeople());
                // Nếu đang FULL và giờ có slot → mở lại
                if (schedule.getStatus() == TourStatus.FULL && schedule.getAvailableSlots() > 0) {
                    schedule.setStatus(TourStatus.OPEN);
                }
                tourScheduleRepository.save(schedule);
            }

            // UC48: Gửi email thông báo cho khách hàng
            User customer = booking.getUser();
            if (customer != null && customer.getEmail() != null) {
                mailService.sendBookingCancelledEmail(
                        customer.getEmail(),
                        customer.getFullName() != null ? customer.getFullName() : "Quý khách",
                        booking.getId(),
                        booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO);
            }
        }

        log.info("[UC47] Đã hủy {} booking PENDING quá hạn (> 24h chưa thanh toán).", unpaidBookings.size());
    }

    // ================================================================
    // UC49: Tự động cộng điểm loyalty cho booking COMPLETED
    // Chạy mỗi ngày lúc 02:00 AM
    // Lưu ý: Hiện tại schema chưa có bảng LoyaltyPoints, nên chỉ log
    // Khi có bảng, implement logic cộng điểm thực tế tại đây
    // ================================================================
    // @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoUpdateLoyaltyPoints() {
        log.info("[UC49] Bắt đầu cộng điểm loyalty cho booking COMPLETED...");

        List<Booking> allBookings = bookingRepository.findAll();
        long completedCount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        // TODO: Khi schema có bảng LoyaltyPoints, implement logic sau:
        // - Mỗi booking COMPLETED → cộng (totalPrice / 10_000) điểm cho user
        // - Lưu vào bảng LoyaltyPoints
        // - Đánh dấu booking đã được cộng điểm (thêm cột PointsAwarded)

        log.info("[UC49] Hiện có {} booking COMPLETED. Sẵn sàng cộng điểm khi bảng LoyaltyPoints được tạo.",
                completedCount);
    }

    // ================================================================
    // UC50: Tạo báo cáo tháng và gửi cho tất cả ADMIN
    // Chạy vào 8:00 AM ngày đầu tiên của mỗi tháng
    // ================================================================
    // @Scheduled(cron = "0 0 8 1 * *")
    @Transactional
    public void generateMonthlyReport() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy tháng trước để báo cáo
        LocalDateTime firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime firstDayOfThisMonth = now.withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        String monthYear = firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        log.info("[UC50] Bắt đầu tạo báo cáo tháng {}...", monthYear);

        // Thống kê booking
        long totalBookings = bookingRepository.findAllInPeriod(firstDayOfLastMonth, firstDayOfThisMonth).size();
        long confirmed = bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.CONFIRMED,
                firstDayOfLastMonth, firstDayOfThisMonth);
        long cancelled = bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.CANCELLED,
                firstDayOfLastMonth, firstDayOfThisMonth);
        long completed = bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.COMPLETED,
                firstDayOfLastMonth, firstDayOfThisMonth);
        BigDecimal revenue = bookingRepository.sumRevenueConfirmedBetween(firstDayOfLastMonth, firstDayOfThisMonth);

        String reportContent = buildReportContent(monthYear, totalBookings, confirmed, cancelled, completed, revenue);

        // Gửi báo cáo cho tất cả tài khoản ADMIN
        List<User> allUsers = userRepository.findAll();
        allUsers.stream()
                .filter(u -> u.getRole() == UserRole.ADMIN && u.getEmail() != null
                        && Boolean.TRUE.equals(u.getIsActive()))
                .forEach(admin -> {
                    mailService.sendMonthlyReportEmail(admin.getEmail(), reportContent, monthYear);
                    log.info("[UC50] Đã gửi báo cáo tháng {} cho admin: {}", monthYear, admin.getEmail());
                });
    }

    private String buildReportContent(String monthYear, long total, long confirmed,
            long cancelled, long completed, BigDecimal revenue) {
        return "========================================\n" +
                "   BÁO CÁO KINH DOANH THÁNG " + monthYear + "\n" +
                "========================================\n\n" +
                "THỐNG KÊ BOOKING:\n" +
                "  - Tổng booking trong tháng  : " + total + "\n" +
                "  - Đã xác nhận (CONFIRMED)   : " + confirmed + "\n" +
                "  - Đã hoàn thành (COMPLETED) : " + completed + "\n" +
                "  - Đã hủy (CANCELLED)        : " + cancelled + "\n\n" +
                "DOANH THU:\n" +
                "  - Tổng doanh thu tháng      : " + String.format("%,.0f", revenue) + " VND\n\n" +
                "========================================\n" +
                "Báo cáo được tạo tự động bởi hệ thống TourBooking.\n" +
                "Ngày tạo: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n";
    }
}
