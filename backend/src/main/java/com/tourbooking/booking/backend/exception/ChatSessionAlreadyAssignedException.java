package com.tourbooking.booking.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném ra khi một phiên chat đã được Staff khác tiếp nhận (không còn ở trạng thái WAITING_STAFF).
 * Trả về HTTP 400 để frontend hiển thị thông báo lỗi trực quan.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ChatSessionAlreadyAssignedException extends RuntimeException {

    public ChatSessionAlreadyAssignedException(Long sessionId) {
        super("Phiên chat #" + sessionId + " đã được tiếp nhận bởi nhân viên khác. Vui lòng tải lại danh sách.");
    }
}
