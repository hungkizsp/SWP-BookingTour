package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupResponse;
import com.tourbooking.booking.backend.model.entity.TourChatGroup;
import com.tourbooking.booking.backend.model.entity.TourChatGroupMember;
import com.tourbooking.booking.backend.model.entity.TourChatGroupMessage;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.repository.TourChatGroupMemberRepository;
import com.tourbooking.booking.backend.repository.TourChatGroupMessageRepository;
import com.tourbooking.booking.backend.repository.TourChatGroupRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.TourChatGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourChatGroupServiceImpl implements TourChatGroupService {

    private final TourChatGroupRepository groupRepository;
    private final TourChatGroupMemberRepository memberRepository;
    private final TourChatGroupMessageRepository messageRepository;
    private final TourScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TourChatGroup getOrCreateGroup(Long scheduleId) {
        return groupRepository.findBySchedule_Id(scheduleId).orElseGet(() -> {
            TourSchedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch trình."));
            TourChatGroup group = TourChatGroup.builder()
                    .schedule(schedule)
                    .isActive(true)
                    .build();
            return groupRepository.save(group);
        });
    }

    @Override
    @Transactional
    public void addMember(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            TourChatGroup group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm chat."));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
            
            TourChatGroupMember member = TourChatGroupMember.builder()
                    .group(group)
                    .user(user)
                    .build();
            memberRepository.save(member);
        }
    }

    @Override
    @Transactional
    public TourChatGroupMessageResponse sendMessage(Long groupId, Long userId, String content) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Người dùng không phải thành viên của nhóm này.");
        }
        TourChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm chat."));
        if (!group.getIsActive()) {
            throw new RuntimeException("Nhóm chat này đã đóng.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        TourChatGroupMessage msg = TourChatGroupMessage.builder()
                .group(group)
                .user(user)
                .content(content)
                .build();
        TourChatGroupMessage saved = messageRepository.save(msg);

        return TourChatGroupMessageResponse.builder()
                .id(saved.getId())
                .groupId(saved.getGroup().getId())
                .userId(saved.getUser().getId())
                .displayName(saved.getUser().getFullName())
                .content(saved.getContent())
                .sentAt(saved.getSentAt())
                .build();
    }

    @Override
    public Page<TourChatGroupMessageResponse> getMessages(Long groupId, Long userId, int page, int size) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Truy cập bị từ chối.");
        }
        return messageRepository.findByGroupId(groupId, PageRequest.of(page, size, Sort.by("sentAt").descending()))
                .map(msg -> TourChatGroupMessageResponse.builder()
                        .id(msg.getId())
                        .groupId(msg.getGroup().getId())
                        .userId(msg.getUser().getId())
                        .displayName(msg.getUser().getFullName())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .build());
    }

    @Override
    public List<TourChatGroupResponse> getMyGroups(Long userId) {
        List<TourChatGroup> groups = groupRepository.findActiveGroupsByUserId(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        return groups.stream()
                .filter(g -> g.getSchedule() != null && g.getSchedule().getStartDate() != null && !g.getSchedule().getStartDate().isBefore(today))
                .map(g -> TourChatGroupResponse.builder()
                        .id(g.getId())
                        .scheduleId(g.getSchedule().getId())
                        .tourName(g.getSchedule().getTour().getTourName())
                        .startDate(g.getSchedule().getStartDate())
                        .memberCount(g.getMembers().size())
                        .isActive(g.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TourChatGroupMemberResponse> getMembers(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Truy cập bị từ chối.");
        }
        return memberRepository.findByGroupId(groupId).stream()
                .map(m -> TourChatGroupMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .displayName(m.getUser().getFullName())
                        .avatar(m.getUser().getAvatarUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void closeGroup(Long scheduleId) {
        groupRepository.findBySchedule_Id(scheduleId).ifPresent(g -> {
            g.setIsActive(false);
            groupRepository.save(g);
        });
    }
}
