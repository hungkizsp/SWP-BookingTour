package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.UserNotificationResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.security.JwtService;
import com.tourbooking.booking.backend.service.UserNotificationService;
import com.tourbooking.booking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService notificationService;
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/my")
    public ApiResponse<List<UserNotificationResponse>> getMyNotifications(Authentication auth) {
        Long userId = currentUser(auth).getId();
        return ApiResponse.<List<UserNotificationResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("OK")
                .data(notificationService.getNotifications(userId))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(Authentication auth) {
        Long userId = currentUser(auth).getId();
        return ApiResponse.<Long>builder()
                .code(HttpStatus.OK.value())
                .message("OK")
                .data(notificationService.countUnread(userId))
                .build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        String email = jwtService.parseClaims(token).getSubject();
        UserResponse user = userService.getUserByEmail(email);
        return notificationService.subscribe(user.getId());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markRead(id, currentUser(auth).getId());
        return ApiResponse.<Void>builder().code(HttpStatus.OK.value()).message("OK").build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(currentUser(auth).getId());
        return ApiResponse.<Void>builder().code(HttpStatus.OK.value()).message("OK").build();
    }

    private UserResponse currentUser(Authentication auth) {
        return userService.getUserByEmail(auth.getName());
    }
}
