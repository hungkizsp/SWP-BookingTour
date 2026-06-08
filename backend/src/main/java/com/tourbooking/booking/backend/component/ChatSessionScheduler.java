package com.tourbooking.booking.backend.component;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tourbooking.booking.backend.model.entity.ChatSession;
import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;
import com.tourbooking.booking.backend.repository.ChatSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 1.1 (tinh chỉnh) — Scheduler giải phóng phiên chat bị treo thông minh.
 * <p>
 * Chạy mỗi 5 phút. Với phiên STAFF_CHATTING không có tương tác > 20 phút:
 * <ul>
 *   <li>Đặt lại trạng thái → WAITING_STAFF (không đóng hẳn)</li>
 *   <li>Xóa AssignedStaffID → NULL (giải phóng khỏi staff mất kết nối)</li>
 *   <li>Phiên hiển thị lại trên dashboard, staff khác có thể tiếp nhận</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSessionScheduler {

    /** Ngưỡng không hoạt động: 20 phút */
    private static final long INACTIVITY_MINUTES = 20L;

    private final ChatSessionRepository chatSessionRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void releaseInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(INACTIVITY_MINUTES);

        List<ChatSession> staleSessions = chatSessionRepository.findByStatusAndLastMessageAtBefore(
                ChatSessionStatus.STAFF_CHATTING, cutoff);

        if (staleSessions.isEmpty()) {
            return;
        }

        log.info("[ChatScheduler] Phát hiện {} phiên treo (> {} phút). Đang giải phóng về WAITING_STAFF...",
                staleSessions.size(), INACTIVITY_MINUTES);

        for (ChatSession session : staleSessions) {
            Long prevStaffId = session.getAssignedStaff() != null ? session.getAssignedStaff().getId() : null;

            // Giải phóng: trả về WAITING_STAFF thay vì CLOSED
            session.setStatus(ChatSessionStatus.WAITING_STAFF);
            session.setAssignedStaff(null);  // Xóa staff đang giữ phiên → staff khác có thể nhận
            chatSessionRepository.save(session);

            log.info("[ChatScheduler] Phiên #{} (prevStaff={}) → WAITING_STAFF (available for re-assignment)",
                    session.getId(), prevStaffId);
        }
    }
}
