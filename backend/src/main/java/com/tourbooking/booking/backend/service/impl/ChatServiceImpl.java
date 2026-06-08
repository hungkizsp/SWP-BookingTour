package com.tourbooking.booking.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tourbooking.booking.backend.model.dto.request.ChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatSessionStatusResponse;
import com.tourbooking.booking.backend.model.entity.ChatMessages;
import com.tourbooking.booking.backend.model.entity.ChatSession;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.ChatSenderType;
import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;
import com.tourbooking.booking.backend.repository.ChatMessagesRepository;
import com.tourbooking.booking.backend.repository.ChatSessionRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.exception.ChatSessionAlreadyAssignedException;
import com.tourbooking.booking.backend.service.ChatNotificationService;
import com.tourbooking.booking.backend.service.ChatService;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final List<String> ESCALATION_KEYWORDS = List.of("nhân viên", "human", "support", "help", "ngoại lệ");
    private static final List<ChatSessionStatus> ACTIVE_STATUSES = List.of(ChatSessionStatus.WAITING_STAFF, ChatSessionStatus.STAFF_CHATTING);

    private final ChatMessagesRepository chatRepo;
    private final ChatSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final ChatNotificationService notificationService;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        ChatSession session = resolveSession(request.getUserId(), request.getGuestId());
        session.setLastMessageAt(LocalDateTime.now());
        if (session.getStatus() == null) {
            session.setStatus(ChatSessionStatus.AI);
        }

        ChatMessages entity = new ChatMessages();
        String sender = normalizeSender(request.getSenderType(), session);
        if (request.getUserId() != null) {
            User user = userRepo.findById(request.getUserId()).orElse(null);
            entity.setUser(user);
        } else {
            entity.setGuestId(request.getGuestId());
        }

        entity.setSenderType(sender);
        entity.setMessage(request.getMessage());
        chatRepo.save(entity);

        boolean escalated = false;
        if ("STAFF".equals(sender)) {
            session.setStatus(ChatSessionStatus.STAFF_CHATTING);
        } else if (shouldEscalate(request.getMessage(), sender)) {
            session.setStatus(ChatSessionStatus.WAITING_STAFF);
            escalated = true;
        }
        session = sessionRepo.save(session);

        if (escalated) {
            publishWaitingNotification(session, entity.getMessage());
        }

        // Real-time: push tin nhắn đến tất cả client đang lắng nghe qua SSE
        ChatMessageResponse response = toResponse(entity);
        try {
            notificationService.publishMessage(response);
        } catch (Exception ex) {
            log.warn("Could not push chat message via SSE: {}", ex.getMessage());
        }
        return response;
    }

    @Override
    public List<ChatMessageResponse> getConversation(Long userId, String guestId) {
        List<ChatMessages> messages;
        if (userId != null) {
            messages = chatRepo.findByUser_IdOrderBySentAtAsc(userId);
        } else if (StringUtils.hasText(guestId)) {
            messages = chatRepo.findByGuestIdOrderBySentAtAsc(guestId);
        } else {
            messages = chatRepo.findByUserIsNullOrderBySentAtAsc();
        }

        return messages.stream().map(ChatServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public void escalateToHuman(Long userId, String guestId) {
        ChatSession session = resolveSession(userId, guestId);
        session.setStatus(ChatSessionStatus.WAITING_STAFF);
        session.setLastMessageAt(LocalDateTime.now());
        session = sessionRepo.save(session);
        publishWaitingNotification(session, latestMessageSnippetForSession(session));
    }

    @Override
    public List<ChatSession> getActiveSessionsForStaff() {
        return sessionRepo.findByStatusInOrderByLastMessageAtDesc(ACTIVE_STATUSES);
    }

    /**
     * 1.1 — Tiếp nhận phiên chat với PESSIMISTIC WRITE LOCK.
     * Nếu phiên không còn ở WAITING_STAFF (đã được staff khác tiếp nhận),
     * ném ra ChatSessionAlreadyAssignedException (HTTP 400).
     */
    @Override
    @Transactional
    public ChatSession assignSession(Long sessionId, Long staffId) {
        ChatSession session = sessionRepo.findByIdWithLock(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên chat #" + sessionId + " không tồn tại."));

        if (session.getStatus() != ChatSessionStatus.WAITING_STAFF) {
            throw new ChatSessionAlreadyAssignedException(sessionId);
        }

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên #" + staffId + " không tồn tại."));

        session.setStatus(ChatSessionStatus.STAFF_CHATTING);
        session.setAssignedStaff(staff); // Gán nhân viên phụ trách
        session.setLastMessageAt(LocalDateTime.now());
        return sessionRepo.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionStatusResponse getSessionStatus(Long userId, String guestId) {
        ChatSession session = findLatestSession(userId, guestId);
        if (session == null || session.getStatus() == null || session.getStatus() == ChatSessionStatus.AI) {
            return ChatSessionStatusResponse.builder()
                    .status(ChatSessionStatus.AI)
                    .build();
        }

        String staffName = session.getAssignedStaff() != null
                ? session.getAssignedStaff().getFullName()
                : null;

        return ChatSessionStatusResponse.builder()
                .id(session.getId())
                .status(session.getStatus())
                .assignedStaffName(staffName)
                .build();
    }

    @Override
    @Transactional
    public void closeSession(Long userId, String guestId) {
        ChatSession session = findLatestSession(userId, guestId);
        if (session == null) {
            return;
        }
        session.setStatus(ChatSessionStatus.CLOSED);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepo.save(session);
    }

    private boolean shouldEscalate(String message, String sender) {
        if (!"GUEST".equals(sender)) {
            return false;
        }
        if (!StringUtils.hasText(message)) return false;
        String lower = message.toLowerCase();
        return ESCALATION_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private void publishWaitingNotification(ChatSession session, String snippet) {
        if (session.getStatus() != ChatSessionStatus.WAITING_STAFF) {
            return;
        }
        String label = buildCustomerLabel(session);
        long waitingCount = sessionRepo.countByStatus(ChatSessionStatus.WAITING_STAFF);
        try {
            notificationService.publish(
                    com.tourbooking.booking.backend.model.dto.response.ChatNotification.builder()
                            .sessionId(session.getId())
                            .userId(session.getUser() == null ? null : session.getUser().getId())
                            .guestId(session.getGuestId())
                            .customerLabel(label)
                            .snippet(snippet)
                            .status(session.getStatus())
                            .timestamp(LocalDateTime.now())
                            .waitingCount(waitingCount)
                            .build()
            );
        } catch (Exception ex) {
            log.warn("Could not push chat notification via SSE: {}", ex.getMessage());
        }
    }

    private String buildCustomerLabel(ChatSession session) {
        if (session.getUser() != null && session.getUser().getFullName() != null) {
            return session.getUser().getFullName();
        }
        if (StringUtils.hasText(session.getGuestId())) {
            return "Guest " + session.getGuestId();
        }
        return "Guest";
    }

    private ChatSession resolveSession(Long userId, String guestId) {
        ChatSession session = findLatestSession(userId, guestId);
        if (session != null && session.getStatus() == ChatSessionStatus.CLOSED) {
            session = createSession(userId, guestId);
        }
        if (session == null) {
            session = createSession(userId, guestId);
        }
        return session;
    }

    private String normalizeSender(String sender, ChatSession session) {
        String normalized = sender == null ? null : sender.trim().toUpperCase();
        if (normalized == null || normalized.isBlank()) {
            normalized = ChatSenderType.GUEST.name();
        }
        try {
            ChatSenderType.valueOf(normalized);
        } catch (Exception e) {
            normalized = ChatSenderType.GUEST.name();
        }
        return normalized;
    }

    private String latestMessageSnippetForSession(ChatSession session) {
        Optional<ChatMessages> latest = Optional.empty();
        if (session.getUser() != null) {
            latest = chatRepo.findTopByUser_IdOrderBySentAtDesc(session.getUser().getId());
        }
        if (latest.isEmpty() && StringUtils.hasText(session.getGuestId())) {
            latest = chatRepo.findTopByGuestIdOrderBySentAtDesc(session.getGuestId());
        }
        if (latest.isEmpty()) {
            latest = chatRepo.findTopByUserIsNullOrderBySentAtDesc();
        }
        return latest.map(ChatMessages::getMessage).orElse("");
    }

    private static ChatMessageResponse toResponse(ChatMessages m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .userId(m.getUser() == null ? null : m.getUser().getId())
                .guestId(m.getGuestId())
                .senderType(m.getSenderType())
                .message(m.getMessage())
                .sentAt(m.getSentAt())
                .build();
    }

    private ChatSession findLatestSession(Long userId, String guestId) {
        ChatSession session = null;
        if (userId != null) {
            session = sessionRepo.findTopByUser_IdOrderByLastMessageAtDesc(userId).orElse(null);
        }
        if (session == null && StringUtils.hasText(guestId)) {
            session = sessionRepo.findTopByGuestIdOrderByLastMessageAtDesc(guestId.trim()).orElse(null);
        }
        return session;
    }

    private ChatSession createSession(Long userId, String guestId) {
        ChatSession session = new ChatSession();
        if (userId != null) {
            userRepo.findById(userId).ifPresent(session::setUser);
        }
        if (StringUtils.hasText(guestId)) {
            session.setGuestId(guestId.trim());
        }
        session.setStatus(ChatSessionStatus.AI);
        session.setLastMessageAt(LocalDateTime.now());
        return session;
    }
}
