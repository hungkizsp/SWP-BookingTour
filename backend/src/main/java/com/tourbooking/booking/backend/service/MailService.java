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
                "đã bị hủy tự động vì không có thanh toán trong vòng 15 phút.\n\n" +
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

    public void sendBookingCreatedEmail(String toEmail, String customerName, Long bookingId, java.math.BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Đặt tour thành công - #" + bookingId);
            message.setText("Xin chào " + customerName + ",\n\nBạn đã đặt tour thành công.\nMã đặt tour: #" + bookingId + "\nTổng tiền: " + amount + " VND\n\nVui lòng hoàn tất thanh toán để giữ chỗ.\n\nTrân trọng,\nĐội ngũ TourBooking");
            mailSender.send(message);
        } catch (Exception e) {}
    }

    public void sendGuideAssignedEmail(String toEmail, String customerName, String tourName, String guideName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Cập nhật hướng dẫn viên cho tour " + tourName);
            message.setText("Xin chào " + customerName + ",\n\nTour của bạn đã được phân công hướng dẫn viên: " + guideName + ".\n\nTrân trọng,\nĐội ngũ TourBooking");
            mailSender.send(message);
        } catch (Exception e) {}
    }
    
    public void sendRefundProcessedEmail(String toEmail, String customerName, Long bookingId, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Kết quả yêu cầu hoàn tiền cho booking #" + bookingId);
            message.setText("Xin chào " + customerName + ",\n\nYêu cầu hoàn tiền của bạn đã được: " + status + ".\n\nTrân trọng,\nĐội ngũ TourBooking");
            mailSender.send(message);
        } catch (Exception e) {}
    }

    private String resolveFromAddress() {
        if (mailFrom == null || mailFrom.isBlank() || mailFrom.contains("your-gmail")) {
            throw new IllegalStateException("spring.mail.username is not configured with a real Gmail address");
        }
        return mailFrom;
    }

    /**
     * Sends an operational alert to the configured mail address (admin/staff).
     * Used by OperationalScheduler when a schedule is missing a guide.
     */
    public void sendOperationalAlert(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(mailFrom); // Alert goes back to the operator/admin mailbox
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Operational alert email sent: {}", subject);
        } catch (Exception e) {
            log.error("Failed to send operational alert '{}': {}", subject, e.getMessage(), e);
        }
    }

    /**
     * Sends a customer-facing notification when their tour is cancelled due to guide unavailability.
     * A full refund has been initiated.
     */
    public void sendOperatorCancellationRefundEmail(
            String toEmail, String customerName, Long bookingId, java.math.BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(toEmail);
            message.setSubject("[TourBooking] Important: Tour Cancellation & Refund for Booking #" + bookingId);
            message.setText(
                "Dear " + customerName + ",\n\n" +
                "We sincerely apologize for the inconvenience.\n\n" +
                "We regret to inform you that your tour booking #" + bookingId +
                " has been cancelled because operational requirements (guide assignment) " +
                "could not be fulfilled before the scheduled departure.\n\n" +
                "A FULL REFUND of " + (amount != null ? amount.toPlainString() : "your payment") + " VND " +
                "has been initiated and will be processed shortly.\n\n" +
                "Reason: Guide Not Assigned\n" +
                "Refund Status: Processing\n\n" +
                "We are truly sorry for this experience. Please contact our support team if you have any questions.\n\n" +
                "Warm regards,\nThe TourBooking Team"
            );
            mailSender.send(message);
            log.info("Operator cancellation refund email sent for booking {} to {}", bookingId, toEmail);
        } catch (Exception e) {
            log.error("Failed to send operator cancellation refund email for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }

    public void sendTourCancellationEmail(String toEmail, String customerName, Long bookingId, String tourName, String reason) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(resolveFromAddress());
            helper.setTo(toEmail);
            helper.setSubject("[TourBooking] Thông báo Hủy đặt tour #" + bookingId);
            
            String htmlMsg = "<h3>Xin chào " + customerName + ",</h3>" +
                    "<p>Lịch trình tour <strong>" + tourName + "</strong> của quý khách đã bị hủy bởi ban quản lý.</p>" +
                    "<p><strong>Lý do hủy:</strong> " + reason + "</p>" +
                    "<p>Nếu bạn đã thanh toán, hệ thống sẽ tiến hành hoàn tiền 100% trong thời gian sớm nhất.</p>" +
                    "<br><p>Trân trọng,<br>Đội ngũ TourBooking</p>";
                    
            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);
            log.info("Cancellation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send cancellation email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
    public void sendTourSuspendedEmailToGuide(String toEmail, String guideName, String tourName, String reason, java.time.LocalDate startDate) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(resolveFromAddress());
            helper.setTo(toEmail);
            helper.setSubject("[TourBooking] Lịch trình tour " + tourName + " tạm ngưng");
            
            String htmlMsg = "<h3>Xin chào " + guideName + ",</h3>" +
                    "<p>Lịch trình tour <strong>" + tourName + "</strong> (khởi hành: " + startDate + ") mà bạn được phân công đã bị <strong>tạm ngưng</strong> bởi ban quản lý.</p>" +
                    "<p><strong>Lý do:</strong> " + reason + "</p>" +
                    "<p>Lịch trình này sẽ không còn hiển thị trong danh sách tour được phân công của bạn cho đến khi có thông báo mới.</p>" +
                    "<br><p>Trân trọng,<br>Đội ngũ TourBooking</p>";
                    
            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);
            log.info("Suspension email sent to guide {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send suspension email to guide {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
