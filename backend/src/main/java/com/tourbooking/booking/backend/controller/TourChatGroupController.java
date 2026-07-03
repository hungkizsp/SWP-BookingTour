package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.SendGroupMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMemberResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupMessageResponse;
import com.tourbooking.booking.backend.model.dto.response.TourChatGroupResponse;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.service.TourChatGroupService;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/chat/groups")
@RequiredArgsConstructor
public class TourChatGroupController {

    private final TourChatGroupService tourChatGroupService;
    private final UserRepository userRepository;
    private final com.tourbooking.booking.backend.service.TourChatEmitterRegistry emitterRegistry;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    @GetMapping("/my")
    public ApiResponse<List<TourChatGroupResponse>> getMyGroups(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<List<TourChatGroupResponse>>builder()
                .data(tourChatGroupService.getMyGroups(userId))
                .build();
    }

    @GetMapping("/{groupId}/messages")
    public ApiResponse<Page<TourChatGroupMessageResponse>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<Page<TourChatGroupMessageResponse>>builder()
                .data(tourChatGroupService.getMessages(groupId, userId, page, size))
                .build();
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<TourChatGroupMemberResponse>> getMembers(
            @PathVariable Long groupId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<List<TourChatGroupMemberResponse>>builder()
                .data(tourChatGroupService.getMembers(groupId, userId))
                .build();
    }

    @PostMapping("/{groupId}/messages")
    public ApiResponse<TourChatGroupMessageResponse> sendMessage(
            @PathVariable Long groupId,
            @RequestBody SendGroupMessageRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        TourChatGroupMessageResponse response = tourChatGroupService.sendMessage(groupId, userId, request.getContent());
        
        // Broadcast via registry to correct group
        emitterRegistry.broadcast(groupId, response);
        
        return ApiResponse.<TourChatGroupMessageResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(value = "/{groupId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessages(@PathVariable Long groupId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            // verify access
            tourChatGroupService.getMembers(groupId, userId);
            
            String emitterKey = userId + "-" + java.util.UUID.randomUUID().toString();
            SseEmitter emitter = emitterRegistry.register(groupId, emitterKey);
            
            try {
                emitter.send(SseEmitter.event().name("connected").data("ok"));
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            
            return emitter;
        } catch (AppException e) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (Exception ex) {
                // Ignore
            }
            emitter.complete();
            return emitter;
        }
    }
}
