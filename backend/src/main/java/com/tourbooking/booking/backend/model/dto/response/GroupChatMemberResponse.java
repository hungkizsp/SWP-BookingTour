package com.tourbooking.booking.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupChatMemberResponse {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String phoneNumber;
}
