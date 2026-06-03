package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.ChatEscalationCloseRequest;
import com.tourbooking.booking.backend.model.dto.request.ChatEscalationRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatEscalationResponse;
import com.tourbooking.booking.backend.service.ChatEscalationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/escalations")
@RequiredArgsConstructor
public class ChatEscalationController {

    private final ChatEscalationService chatEscalationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatEscalationResponse> requestEscalation(@Valid @RequestBody ChatEscalationRequest request) {
        ChatEscalationResponse response = chatEscalationService.requestEscalation(request);
        return ApiResponse.<ChatEscalationResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Escalation requested")
                .data(response)
                .build();
    }

    @GetMapping("/active")
    public ApiResponse<ChatEscalationResponse> getActiveEscalation(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestId) {
        ChatEscalationResponse response = chatEscalationService.fetchActiveEscalation(userId, guestId);
        return ApiResponse.<ChatEscalationResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Active escalation")
                .data(response)
                .build();
    }

    @PostMapping("/close")
    public ApiResponse<ChatEscalationResponse> closeEscalation(@Valid @RequestBody ChatEscalationCloseRequest request) {
        if (request.getUserId() == null && !StringUtils.hasText(request.getGuestId())) {
            throw new IllegalArgumentException("Please provide a userId or guestId.");
        }
        ChatEscalationResponse response = chatEscalationService.closeByRequester(request.getUserId(), request.getGuestId());
        return ApiResponse.<ChatEscalationResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Escalation closed")
                .data(response)
                .build();
    }
}
