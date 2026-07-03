package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupResponse;
import com.tourbooking.booking.backend.model.entity.TourChatGroup;
import com.tourbooking.booking.backend.model.entity.TourChatGroupMessage;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TourChatGroupService {
    TourChatGroup getOrCreateGroup(Long scheduleId);
    void addMember(Long groupId, Long userId);
    TourChatGroupMessageResponse sendMessage(Long groupId, Long userId, String content);
    Page<TourChatGroupMessageResponse> getMessages(Long groupId, Long userId, int page, int size);
    List<TourChatGroupResponse> getMyGroups(Long userId);
    List<TourChatGroupMemberResponse> getMembers(Long groupId, Long userId);
    void closeGroup(Long scheduleId);
}
