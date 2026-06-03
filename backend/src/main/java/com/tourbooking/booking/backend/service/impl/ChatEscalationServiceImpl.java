package com.tourbooking.booking.backend.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tourbooking.booking.backend.model.dto.request.ChatEscalationReplyRequest;
import com.tourbooking.booking.backend.model.dto.request.ChatEscalationRequest;
import com.tourbooking.booking.backend.model.dto.response.ChatEscalationResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatEscalationSummaryResponse;
import com.tourbooking.booking.backend.model.entity.ChatEscalation;
import com.tourbooking.booking.backend.model.entity.ChatMessages;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.ChatEscalationStatus;
import com.tourbooking.booking.backend.model.entity.enums.ChatSenderType;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.ChatEscalationRepository;
import com.tourbooking.booking.backend.repository.ChatMessagesRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.ChatEscalationService;
import com.tourbooking.booking.backend.service.ChatService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatEscalationServiceImpl implements ChatEscalationService {

    private static final List<ChatEscalationStatus> ACTIVE_STATUSES = List.of(ChatEscalationStatus.OPEN, ChatEscalationStatus.IN_REVIEW);

    private final ChatEscalationRepository escalationRepo;
    private final ChatMessagesRepository chatRepo;
    private final UserRepository userRepo;
    private final ChatService chatService;

    @Override
    @Transactional
    public ChatEscalationResponse requestEscalation(ChatEscalationRequest request) {
        if (request.getUserId() == null && !StringUtils.hasText(request.getGuestId())) {
            throw new IllegalArgumentException("Please provide a userId or guestId.");
        }

        Optional<ChatEscalation> existing = findActiveEscalation(request.getUserId(), request.getGuestId());
        ChatEscalation escalation = existing.orElseGet(ChatEscalation::new);

        if (request.getUserId() != null) {
            userRepo.findById(request.getUserId()).ifPresent(escalation::setCustomer);
            escalation.setGuestId(null);
        } else if (StringUtils.hasText(request.getGuestId())) {
            escalation.setGuestId(request.getGuestId().trim());
            escalation.setCustomer(null);
        }

        if (StringUtils.hasText(request.getRequestNote())) {
            escalation.setRequestNote(request.getRequestNote().trim());
        }

        if (StringUtils.hasText(request.getMeetingPreference())) {
            escalation.setMeetingPreference(request.getMeetingPreference().trim());
        }

        escalation.setStatus(ChatEscalationStatus.OPEN);
        ChatEscalation saved = escalationRepo.save(escalation);
        chatService.escalateToHuman(request.getUserId(), request.getGuestId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatEscalationSummaryResponse> listActiveEscalations() {
        List<ChatEscalation> escalations = escalationRepo.findByStatusInOrderByUpdatedAtDesc(ACTIVE_STATUSES);
        return escalations.stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public ChatEscalationResponse reply(Long escalationId, Long staffId, ChatEscalationReplyRequest request) {
        ChatEscalation escalation = escalationRepo.findById(escalationId)
                .orElseThrow(() -> new IllegalArgumentException("Escalation not found"));

        User staff = requireAdmin(staffId);

        ChatMessages staffMsg = new ChatMessages();
        staffMsg.setUser(staff);
        staffMsg.setGuestId(escalation.getGuestId());
        staffMsg.setSenderType(ChatSenderType.STAFF.name());
        staffMsg.setMessage(request.getMessage().trim());
        chatRepo.save(staffMsg);

        escalation.setAssignedStaff(staff);
        escalation.setStatus(ChatEscalationStatus.IN_REVIEW);
        return toResponse(escalationRepo.save(escalation));
    }

    @Override
    @Transactional
    public ChatEscalationResponse resolve(Long escalationId, Long staffId) {
        ChatEscalation escalation = escalationRepo.findById(escalationId)
                .orElseThrow(() -> new IllegalArgumentException("Escalation not found"));

        User staff = requireAdmin(staffId);
        escalation.setAssignedStaff(staff);
        escalation.setStatus(ChatEscalationStatus.RESOLVED);
        return toResponse(escalationRepo.save(escalation));
    }

    @Override
    @Transactional
    public ChatEscalationResponse assign(Long escalationId, Long staffId) {
        ChatEscalation escalation = escalationRepo.findById(escalationId)
                .orElseThrow(() -> new IllegalArgumentException("Escalation not found"));

        User staff = requireAdmin(staffId);
        escalation.setAssignedStaff(staff);
        if (escalation.getStatus() != ChatEscalationStatus.RESOLVED) {
            escalation.setStatus(ChatEscalationStatus.IN_REVIEW);
        }
        return toResponse(escalationRepo.save(escalation));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatEscalationResponse fetchActiveEscalation(Long userId, String guestId) {
        return findActiveEscalation(userId, guestId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public ChatEscalationResponse closeByRequester(Long userId, String guestId) {
        ChatEscalation escalation = findActiveEscalation(userId, guestId)
                .orElseThrow(() -> new IllegalArgumentException("No active escalation to close"));

        if (!matchesRequester(escalation, userId, guestId)) {
            throw new IllegalArgumentException("Cannot close escalation you do not own.");
        }

        chatService.closeSession(userId, guestId);
        escalation.setStatus(ChatEscalationStatus.RESOLVED);
        ChatEscalation saved = escalationRepo.save(escalation);
        return toResponse(saved);
    }

    private Optional<ChatEscalation> findActiveEscalation(Long userId, String guestId) {
        if (userId != null) {
            return escalationRepo.findTopByCustomer_IdAndStatusNotInOrderByUpdatedAtDesc(userId, List.of(ChatEscalationStatus.RESOLVED));
        }
        if (StringUtils.hasText(guestId)) {
            return escalationRepo.findTopByGuestIdAndStatusNotInOrderByUpdatedAtDesc(guestId.trim(), List.of(ChatEscalationStatus.RESOLVED));
        }
        return Optional.empty();
    }

    private boolean matchesRequester(ChatEscalation escalation, Long userId, String guestId) {
        if (userId != null && escalation.getCustomer() != null && escalation.getCustomer().getId().equals(userId)) {
            return true;
        }
        String trimmedGuestId = guestId == null ? null : guestId.trim();
        return StringUtils.hasText(trimmedGuestId) && trimmedGuestId.equals(escalation.getGuestId());
    }

    private ChatEscalationResponse toResponse(ChatEscalation escalation) {
        return ChatEscalationResponse.builder()
                .id(escalation.getId())
                .userId(escalation.getCustomer() == null ? null : escalation.getCustomer().getId())
                .guestId(escalation.getGuestId())
                .status(escalation.getStatus())
                .meetingPreference(escalation.getMeetingPreference())
                .requestNote(escalation.getRequestNote())
                .assignedStaffId(escalation.getAssignedStaff() == null ? null : escalation.getAssignedStaff().getId())
                .assignedStaffName(escalation.getAssignedStaff() == null ? null : escalation.getAssignedStaff().getFullName())
                .createdAt(escalation.getCreatedAt())
                .updatedAt(escalation.getUpdatedAt())
                .build();
    }

    private ChatEscalationSummaryResponse toSummary(ChatEscalation escalation) {
        String customerLabel = escalation.getCustomer() != null
                ? escalation.getCustomer().getFullName()
                : (escalation.getGuestId() != null ? "Guest " + escalation.getGuestId() : "Guest");

        return ChatEscalationSummaryResponse.builder()
                .id(escalation.getId())
                .userId(escalation.getCustomer() == null ? null : escalation.getCustomer().getId())
                .guestId(escalation.getGuestId())
                .customerLabel(customerLabel)
                .status(escalation.getStatus())
                .meetingPreference(escalation.getMeetingPreference())
                .requestNote(escalation.getRequestNote())
                .latestMessageSnippet(latestSnippet(escalation))
                .assignedStaffName(escalation.getAssignedStaff() == null ? null : escalation.getAssignedStaff().getFullName())
                .updatedAt(escalation.getUpdatedAt())
                .build();
    }

    private String latestSnippet(ChatEscalation escalation) {
        return findLatestMessage(escalation)
                .map(ChatMessages::getMessage)
                .orElse("No messages yet");
    }

    private Optional<ChatMessages> findLatestMessage(ChatEscalation escalation) {
        if (escalation.getCustomer() != null) {
            return chatRepo.findTopByUser_IdOrderBySentAtDesc(escalation.getCustomer().getId());
        }
        if (StringUtils.hasText(escalation.getGuestId())) {
            return chatRepo.findTopByGuestIdOrderBySentAtDesc(escalation.getGuestId());
        }
        return chatRepo.findTopByUserIsNullOrderBySentAtDesc();
    }

    private User requireAdmin(Long staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff user must be provided.");
        }
        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));
        if (staff.getRole() == null || staff.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can perform this action.");
        }
        return staff;
    }
}
