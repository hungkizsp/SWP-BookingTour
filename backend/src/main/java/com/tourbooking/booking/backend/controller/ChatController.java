package com.tourbooking.booking.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourbooking.booking.backend.model.dto.request.ChatMessageRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.ChatMessageResponse;
import com.tourbooking.booking.backend.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // UC11: guest chat support - send a message
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageResponse> send(@Valid @RequestBody ChatMessageRequest request) {
        return ApiResponse.<ChatMessageResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Message sent")
                .data(chatService.sendMessage(request))
                .build();
    }

    // UC11: get conversation history (userId or guestId)
    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageResponse>> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Conversation history")
                .data(chatService.getConversation(userId, guestId))
                .build();
    }
}

