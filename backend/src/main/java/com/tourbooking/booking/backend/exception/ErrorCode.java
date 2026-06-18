package com.tourbooking.booking.backend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error"),
    USER_NOT_FOUND(1001, "User not found"),
    TOUR_NOT_FOUND(1002, "Tour not found"),
    CATEGORY_NOT_FOUND(1003, "Category not found"),
    REVIEW_NOT_FOUND(1004, "Review not found"),
    BOOKING_NOT_FOUND(1005, "Booking not found"),
    NEWSLETTER_NOT_FOUND(1006, "Newsletter subscription not found"),
    EMAIL_EXISTED(1007, "Email already exists"),
    INVALID_RATING(1008, "Rating must be between 1 and 5"),
    UNAUTHORIZED(1009, "Unauthorized access"),
    FORBIDDEN(1010, "Access denied"),
    INVALID_REQUEST(1011, "Invalid request"),
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
    TOUR_NOT_COMPLETED_YET(1020, "Chuyến đi chưa hoàn thành. Bạn chỉ có thể đánh giá sau khi tour kết thúc.");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
