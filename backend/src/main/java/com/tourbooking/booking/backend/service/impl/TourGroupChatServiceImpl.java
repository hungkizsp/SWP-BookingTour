package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.response.GroupChatMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatSummaryResponse;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.TourGroupMessage;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.TourGroupMessageRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.service.GroupChatNotificationService;
import com.tourbooking.booking.backend.service.TourGroupChatService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourGroupChatServiceImpl implements TourGroupChatService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.CONFIRMED, BookingStatus.SUCCESS, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);

    private final TourScheduleRepository tourScheduleRepository;
    private final BookingRepository bookingRepository;
    private final TourGroupMessageRepository tourGroupMessageRepository;
    private final GroupChatNotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long scheduleId, Long userId, UserRole role) {
        if (role == UserRole.ADMIN || role == UserRole.STAFF) {
            return true;
        }
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return false;
        }
        if (role == UserRole.GUIDE) {
            return schedule.getGuide() != null && schedule.getGuide().getId().equals(userId);
        }
        if (role == UserRole.CUSTOMER) {
            return bookingRepository.findByScheduleId(scheduleId).stream()
                    .anyMatch(b -> b.getUser() != null && b.getUser().getId().equals(userId)
                            && ACTIVE_STATUSES.contains(b.getStatus()));
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupChatMemberResponse> getMembers(Long scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Map<Long, GroupChatMemberResponse> members = new LinkedHashMap<>();

        if (schedule.getGuide() != null) {
            User guide = schedule.getGuide();
            members.put(guide.getId(), GroupChatMemberResponse.builder()
                    .userId(guide.getId())
                    .fullName(guide.getFullName())
                    .avatarUrl(guide.getAvatarUrl())
                    .role("GUIDE")
                    .phoneNumber(guide.getPhoneNumber())
                    .build());
        }

        bookingRepository.findByScheduleId(scheduleId).stream()
                .filter(b -> b.getUser() != null && ACTIVE_STATUSES.contains(b.getStatus()))
                .map(Booking::getUser)
                .forEach(u -> members.putIfAbsent(u.getId(), GroupChatMemberResponse.builder()
                        .userId(u.getId())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .role("CUSTOMER")
                        .phoneNumber(u.getPhoneNumber())
                        .build()));

        return List.copyOf(members.values());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupChatSummaryResponse getScheduleInfo(Long scheduleId, Long requesterId, UserRole requesterRole) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!isMember(scheduleId, requesterId, requesterRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập nhóm chat của lịch trình này.");
        }
        return toSummary(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupChatMessageResponse> getMessages(Long scheduleId, Long requesterId, UserRole requesterRole) {
        if (!tourScheduleRepository.existsById(scheduleId)) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_FOUND);
        }
        if (!isMember(scheduleId, requesterId, requesterRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập nhóm chat của lịch trình này.");
        }
        return tourGroupMessageRepository.findBySchedule_IdOrderBySentAtAsc(scheduleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GroupChatMessageResponse sendMessage(Long scheduleId, Long senderId, UserRole senderRole, String content) {
        TourSchedule schedule = tourScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (senderRole == UserRole.ADMIN || senderRole == UserRole.STAFF) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Staff/Admin chỉ được xem, không thể gửi tin nhắn trong nhóm chat của tour.");
        }
        if (!isMember(scheduleId, senderId, senderRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền gửi tin nhắn vào nhóm chat này.");
        }

        User sender = senderRole == UserRole.GUIDE ? schedule.getGuide()
                : bookingRepository.findByScheduleId(scheduleId).stream()
                        .map(Booking::getUser)
                        .filter(u -> u != null && u.getId().equals(senderId))
                        .findFirst()
                        .orElse(null);
        if (sender == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Không xác định được người gửi trong nhóm chat này.");
        }

        TourGroupMessage entity = new TourGroupMessage();
        entity.setSchedule(schedule);
        entity.setSender(sender);
        entity.setSenderRole(senderRole.name());
        entity.setMessage(content);
        TourGroupMessage saved = tourGroupMessageRepository.save(entity);

        GroupChatMessageResponse response = toResponse(saved);
        notificationService.publish(scheduleId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupChatSummaryResponse> getMyGroups(Long userId, UserRole role) {
        if (role == UserRole.CUSTOMER) {
            Map<Long, TourSchedule> schedules = bookingRepository.findByUser_IdAndStatusIn(userId, ACTIVE_STATUSES)
                    .stream()
                    .map(Booking::getSchedule)
                    .filter(s -> s != null)
                    .collect(Collectors.toMap(TourSchedule::getId, s -> s, (a, b) -> a, LinkedHashMap::new));
            return schedules.values().stream()
                    .sorted(Comparator.comparing(TourSchedule::getStartDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }
        if (role == UserRole.GUIDE) {
            return tourScheduleRepository.findByGuide_Id(userId).stream()
                    .sorted(Comparator.comparing(TourSchedule::getStartDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private GroupChatSummaryResponse toSummary(TourSchedule schedule) {
        List<TourGroupMessage> history = tourGroupMessageRepository.findBySchedule_IdOrderBySentAtAsc(schedule.getId());
        TourGroupMessage last = history.isEmpty() ? null : history.get(history.size() - 1);

        String tourImage = null;
        if (schedule.getTour() != null && schedule.getTour().getImages() != null
                && !schedule.getTour().getImages().isEmpty()) {
            tourImage = schedule.getTour().getImages().get(0).getImageUrl();
        }

        return GroupChatSummaryResponse.builder()
                .scheduleId(schedule.getId())
                .tourName(schedule.getTour() != null ? schedule.getTour().getTourName() : null)
                .tourImage(tourImage)
                .departureDate(schedule.getStartDate())
                .returnDate(schedule.getEndDate())
                .guideId(schedule.getGuide() != null ? schedule.getGuide().getId() : null)
                .guideName(schedule.getGuide() != null ? schedule.getGuide().getFullName() : null)
                .scheduleStatus(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                .lastMessage(last != null ? last.getMessage() : null)
                .lastMessageAt(last != null ? last.getSentAt() : null)
                .build();
    }

    private GroupChatMessageResponse toResponse(TourGroupMessage msg) {
        User sender = msg.getSender();
        return GroupChatMessageResponse.builder()
                .id(msg.getId())
                .scheduleId(msg.getSchedule() != null ? msg.getSchedule().getId() : null)
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? sender.getFullName() : null)
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                .senderRole(msg.getSenderRole())
                .message(msg.getMessage())
                .sentAt(msg.getSentAt())
                .build();
    }
}
