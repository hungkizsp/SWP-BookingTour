package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourChatGroupMessageResponse {
    private Long id;
    private Long groupId;
    private Long userId;
    private String displayName;
    private String content;
    private LocalDateTime sentAt;
}
