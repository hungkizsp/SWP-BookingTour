package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.GroupChatMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatSummaryResponse;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;

import java.util.List;

public interface TourGroupChatService {

    /**
     * Quyền vào nhóm chat được tính động: CUSTOMER có booking active trên schedule này,
     * GUIDE được phân công cho schedule này, hoặc STAFF/ADMIN (xem mọi nhóm để giám sát).
     */
    boolean isMember(Long scheduleId, Long userId, UserRole role);

    List<GroupChatMemberResponse> getMembers(Long scheduleId);

    GroupChatSummaryResponse getScheduleInfo(Long scheduleId, Long requesterId, UserRole requesterRole);

    List<GroupChatMessageResponse> getMessages(Long scheduleId, Long requesterId, UserRole requesterRole);

    GroupChatMessageResponse sendMessage(Long scheduleId, Long senderId, UserRole senderRole, String content);

    List<GroupChatSummaryResponse> getMyGroups(Long userId, UserRole role);
}
