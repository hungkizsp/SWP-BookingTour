package com.tourbooking.booking.backend.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import com.tourbooking.booking.backend.model.dto.request.ChatEscalationReplyRequest;
import com.tourbooking.booking.backend.model.dto.request.ChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatSessionSummaryResponse;
import com.tourbooking.booking.backend.model.entity.ChatSession;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;
import com.tourbooking.booking.backend.repository.ChatSessionRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.ChatNotificationService;
import com.tourbooking.booking.backend.service.ChatService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/admin/chat/escalations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class AdminChatController {

    private final ChatService chatService;
    private final ChatSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ChatNotificationService notificationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatSessionSummaryResponse>> listEscalations() {
        List<ChatSession> sessions = chatService.getActiveSessionsForStaff();
        List<ChatSessionSummaryResponse> data = sessions.stream()
                .map(this::toSummary)
                .toList();
        return ApiResponse.<List<ChatSessionSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Waiting sessions")
                .data(data)
                .build();
    }

    @PostMapping("/{id}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<ChatSessionSummaryResponse> reply(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChatEscalationReplyRequest request,
            Principal principal) {
        resolveStaffId(principal);
        ChatSession session = sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        ChatMessageRequest msg = new ChatMessageRequest();
        msg.setUserId(session.getUser() == null ? null : session.getUser().getId());
        msg.setGuestId(session.getGuestId());
        msg.setSenderType("STAFF");
        msg.setMessage(request.getMessage());
        chatService.sendMessage(msg);

        session.setStatus(ChatSessionStatus.STAFF_CHATTING);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(session);

        return ApiResponse.<ChatSessionSummaryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Reply posted")
                .data(toSummary(session))
                .build();
    }

    @PostMapping("/{id}/assign")
    @Transactional
    public ApiResponse<ChatSessionSummaryResponse> assign(@PathVariable("id") Long id, Principal principal) {
        resolveStaffId(principal);
        ChatSession session = sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setStatus(ChatSessionStatus.STAFF_CHATTING);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(session);
        return ApiResponse.<ChatSessionSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Session joined")
                .data(toSummary(session))
                .build();
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public ApiResponse<ChatSessionSummaryResponse> resolve(@PathVariable("id") Long id, Principal principal) {
        resolveStaffId(principal);
        ChatSession session = sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setStatus(ChatSessionStatus.CLOSED);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(session);
        return ApiResponse.<ChatSessionSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Session closed")
                .data(toSummary(session))
                .build();
    }

    @GetMapping("/notifications/stream")
    public SseEmitter notifications() {
        return notificationService.subscribe();
    }

    private ChatSessionSummaryResponse toSummary(ChatSession session) {
        String label = session.getUser() != null
                ? session.getUser().getFullName()
                : (session.getGuestId() != null ? "Guest " + session.getGuestId() : "Guest");
        return ChatSessionSummaryResponse.builder()
                .id(session.getId())
                .userId(session.getUser() == null ? null : session.getUser().getId())
                .guestId(session.getGuestId())
                .customerLabel(label)
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt())
                .build();
    }

    private Long resolveStaffId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalArgumentException("Staff identity is required.");
        }
        User staff = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));
        return staff.getId();
    }
}
