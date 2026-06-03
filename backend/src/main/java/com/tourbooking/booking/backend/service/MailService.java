package com.tourbooking.booking.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("Verify your email - TourBooking");

            String link = "http://localhost:8080/api/v1/auth/verify?token=" + token;

            message.setText("Click to verify your email: " + link);

            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("Password Reset - TourBooking");
            message.setText("Reset link: " + resetUrl);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // UC48: Gửi email thông báo hủy booking tự động do chưa thanh toán
    public void sendBookingCancelledEmail(String toEmail, String customerName, Long bookingId, java.math.BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Booking #" + bookingId + " đã bị hủy");
            message.setText(
                "Xin chào " + customerName + ",\n\n" +
                "Booking #" + bookingId + " của bạn (tổng tiền: " + amount + " VND) " +
                "đã bị hủy tự động vì không có thanh toán trong vòng 24 giờ.\n\n" +
                "Nếu bạn vẫn muốn đặt tour, vui lòng truy cập lại website.\n\n" +
                "Trân trọng,\nĐội ngũ TourBooking"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }

    // UC50: Gửi báo cáo tháng cho admin
    public void sendMonthlyReportEmail(String toEmail, String reportContent, String monthYear) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Báo cáo tháng " + monthYear);
            message.setText(reportContent);
            mailSender.send(message);
            log.info("Monthly report email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send monthly report email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPaymentSuccessEmail(String toEmail, String customerName, Long bookingId, java.math.BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Thanh toan thanh cong cho booking #" + bookingId);
            message.setText(
                    "Xin chao " + customerName + ",\n\n" +
                            "Thanh toan cho booking #" + bookingId + " da thanh cong.\n" +
                            "So tien: " + amount + " VND.\n\n" +
                            "Cam on ban da su dung TourBooking."
            );
            mailSender.send(message);
            log.info("Payment success email sent for booking {} to {}", bookingId, toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment success email for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }

    public void sendBookingConfirmedEmail(String toEmail, String customerName, Long bookingId, java.math.BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Booking #" + bookingId + " da duoc xac nhan");
            message.setText(
                    "Xin chao " + customerName + ",\n\n" +
                            "Booking #" + bookingId + " cua ban da duoc xac nhan.\n" +
                            "Tong tien: " + amount + " VND.\n\n" +
                            "Cam on ban da su dung TourBooking."
            );
            mailSender.send(message);
            log.info("Booking confirmation email sent for booking {} to {}", bookingId, toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }

    private String resolveFromAddress() {
        if (mailFrom == null || mailFrom.isBlank() || mailFrom.contains("your-gmail")) {
            throw new IllegalStateException("spring.mail.username is not configured with a real Gmail address");
        }
        return mailFrom;
    }
}
