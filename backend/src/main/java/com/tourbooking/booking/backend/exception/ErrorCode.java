package com.tourbooking.booking.backend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định."),
    USER_NOT_FOUND(1001, "Không tìm thấy người dùng."),
    TOUR_NOT_FOUND(1002, "Không tìm thấy tour."),
    CATEGORY_NOT_FOUND(1003, "Không tìm thấy danh mục."),
    REVIEW_NOT_FOUND(1004, "Không tìm thấy đánh giá."),
    BOOKING_NOT_FOUND(1005, "Không tìm thấy booking."),
    NEWSLETTER_NOT_FOUND(1006, "Không tìm thấy đăng ký nhận bản tin."),
    EMAIL_EXISTED(1007, "Email đã được đăng ký."),
    INVALID_RATING(1008, "Điểm đánh giá phải từ 1 đến 5."),
    UNAUTHORIZED(1009, "Bạn không có quyền truy cập."),
    FORBIDDEN(1010, "Truy cập bị từ chối."),
    INVALID_REQUEST(1011, "Yêu cầu không hợp lệ."),
    PAYOS_PAYMENT_PENDING(1013, "PayOS chưa ghi nhận thanh toán (PENDING/PROCESSING). Vui lòng thử lại sau vài giây."),
    PAYOS_NOT_CONFIGURED(1014,
            "Chưa cấu hình PayOS: đặt PAYOS_CLIENT_ID và PAYOS_API_KEY trong .env rồi khởi động lại backend."),
    PAYOS_LINK_FAILED(1015,
            "PayOS không tạo được link (sai Client ID/API Key, sai số tiền, hoặc lỗi mạng). Kiểm tra my.payos.vn và log backend."),
    VNPAY_NOT_CONFIGURED(1016,
            "Chưa cấu hình VNPay: đặt VNP_TMN_CODE và VNP_HASH_SECRET trong .env rồi khởi động lại backend."),
    VNPAY_SIGNATURE_INVALID(1017, "Chữ ký VNPay không hợp lệ."),
    VNPAY_PAYMENT_FAILED(1018, "Thanh toán VNPay không thành công."),
    REVIEW_ALREADY_EXISTS(1019, "Đặt tour này đã có đánh giá. Mỗi đặt tour chỉ được đánh giá một lần."),
    TOUR_NOT_COMPLETED_YET(1020, "Chuyến đi chưa hoàn thành. Bạn chỉ có thể đánh giá sau khi tour kết thúc."),
    SCHEDULE_NOT_FOUND(1021, "Lịch trình không tồn tại."),
    SCHEDULE_NOT_BOOKABLE(1022, "Lịch trình này không thể đặt (đã hủy hoặc đã hoàn thành)."),
    TOUR_ALREADY_STARTED(1023, "Tour đã bắt đầu, không thể đặt chỗ."),
    BOOKING_DEADLINE_PASSED(1024, "Đã quá hạn đặt tour. Vui lòng chọn lịch khác."),
    SCHEDULE_SOLD_OUT(1025, "Tour đã hết chỗ."),
    INSUFFICIENT_SLOTS(1026, "Số chỗ trống không đủ cho số lượng hành khách yêu cầu."),
    SCHEDULE_PENDING_GUIDE(1027, "Lịch trình này tạm thời không thể đặt vì chưa được phân công hướng dẫn viên."),
    RATE_LIMIT_EXCEEDED(1028, "Tần suất gửi yêu cầu quá nhanh. Vui lòng thử lại sau ít phút."),
    DUPLICATE_BOOKING(1029, "Yêu cầu đặt tour trùng lặp. Vui lòng thử lại sau 30 giây."),
    IP_BLOCKED(1030, "Địa chỉ IP của bạn tạm thời bị khóa do có hành vi gửi yêu cầu bất thường.");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
