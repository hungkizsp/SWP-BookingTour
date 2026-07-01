package com.tourbooking.booking.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tourbooking.booking.backend.model.dto.request.GroupChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.response.GroupChatMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.GroupChatSummaryResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.service.GroupChatNotificationService;
import com.tourbooking.booking.backend.service.TourGroupChatService;
import com.tourbooking.booking.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Nhóm chat theo TourSchedule: khách hàng đã CONFIRMED, hướng dẫn viên được phân công,
 * và Staff/Admin (xem read-only để giám sát) đều truy cập qua đây.
 */
@RestController
@RequestMapping("/api/v1/group-chats")
@RequiredArgsConstructor
public class GroupChatController {

    private final TourGroupChatService groupChatService;
    private final GroupChatNotificationService notificationService;
    private final UserService userService;

    @GetMapping("/my-groups")
    public ApiResponse<List<GroupChatSummaryResponse>> myGroups(Authentication authentication) {
        UserResponse user = currentUser(authentication);
        return ApiResponse.<List<GroupChatSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("My group chats retrieved")
                .data(groupChatService.getMyGroups(user.getId(), user.getRole()))
                .build();
    }

    @GetMapping("/{scheduleId}/messages")
    public ApiResponse<List<GroupChatMessageResponse>> messages(
            @PathVariable Long scheduleId, Authentication authentication) {
        UserResponse user = currentUser(authentication);
        return ApiResponse.<List<GroupChatMessageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Group chat history")
                .data(groupChatService.getMessages(scheduleId, user.getId(), user.getRole()))
                .build();
    }

    @PostMapping("/{scheduleId}/messages")
    public ApiResponse<GroupChatMessageResponse> send(
            @PathVariable Long scheduleId,
            @Valid @RequestBody GroupChatMessageRequest request,
            Authentication authentication) {
        UserResponse user = currentUser(authentication);
        GroupChatMessageResponse saved = groupChatService.sendMessage(
                scheduleId, user.getId(), user.getRole(), request.getMessage());
        return ApiResponse.<GroupChatMessageResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Message sent")
                .data(saved)
                .build();
    }

    @GetMapping("/{scheduleId}/info")
    public ApiResponse<GroupChatSummaryResponse> scheduleInfo(
            @PathVariable Long scheduleId, Authentication authentication) {
        UserResponse user = currentUser(authentication);
        return ApiResponse.<GroupChatSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Group chat schedule info")
                .data(groupChatService.getScheduleInfo(scheduleId, user.getId(), user.getRole()))
                .build();
    }

    @GetMapping("/{scheduleId}/members")
    public ApiResponse<List<GroupChatMemberResponse>> members(
            @PathVariable Long scheduleId, Authentication authentication) {
        UserResponse user = currentUser(authentication);
        if (!groupChatService.isMember(scheduleId, user.getId(), user.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập nhóm chat của lịch trình này.");
        }
        return ApiResponse.<List<GroupChatMemberResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Group chat members")
                .data(groupChatService.getMembers(scheduleId))
                .build();
    }

    /**
     * SSE — frontend kết nối qua EventSource("/api/v1/group-chats/{id}/stream?token=...");
     * JwtAuthenticationFilter đã hỗ trợ fallback đọc token từ query param.
     */
    @GetMapping("/{scheduleId}/stream")
    public SseEmitter stream(@PathVariable Long scheduleId, Authentication authentication) {
        UserResponse user = currentUser(authentication);
        if (!groupChatService.isMember(scheduleId, user.getId(), user.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập nhóm chat của lịch trình này.");
        }
        return notificationService.subscribe(scheduleId);
    }

    private UserResponse currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }
}
