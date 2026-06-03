package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        // Sửa sessionId viết thường theo đúng repository
        long onlineUsers = userRepository.countByCurrentSessionIdIsNotNull(); 
        
        // Đếm đơn hàng theo Enum
        long totalBookings = bookingRepository.countByStatus(com.tourbooking.booking.backend.model.entity.enums.BookingStatus.CONFIRMED) 
                           + bookingRepository.countByStatus(com.tourbooking.booking.backend.model.entity.enums.BookingStatus.PENDING);
        
        // Tính doanh thu tháng hiện tại
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Double monthlyRevenue = paymentRepository.sumSuccessfulPaymentsAfter(startOfMonth);
        if (monthlyRevenue == null) monthlyRevenue = 0.0;

        stats.put("totalUsers", totalUsers);
        stats.put("onlineUsers", onlineUsers);
        stats.put("totalBookings", totalBookings);
        stats.put("totalRevenue", monthlyRevenue);
        
        return stats;
    }
}
