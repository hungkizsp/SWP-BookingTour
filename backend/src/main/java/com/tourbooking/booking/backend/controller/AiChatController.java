package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.AiChatRequest;
import com.tourbooking.booking.backend.model.dto.response.AiChatResponse;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.entity.ChatSession;
import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;
import com.tourbooking.booking.backend.repository.ChatSessionRepository;
import com.tourbooking.booking.backend.service.AiChatService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ChatSessionRepository sessionRepository;

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        ChatSession session = findSession(request);
        if (session != null && session.getStatus() != ChatSessionStatus.AI) {
            return ApiResponse.<AiChatResponse>builder()
                    .code(HttpStatus.OK.value())
                    .message("Staff is handling the conversation")
                    .build();
        }

        AiChatResponse response = aiChatService.chat(request);
        return ApiResponse.<AiChatResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("AI chat response")
                .data(response)
                .build();
    }

    private ChatSession findSession(AiChatRequest request) {
        if (request.getUserId() != null) {
            return sessionRepository.findTopByUser_IdOrderByLastMessageAtDesc(request.getUserId()).orElse(null);
        }
        if (request.getGuestId() != null && !request.getGuestId().isBlank()) {
            return sessionRepository.findTopByGuestIdOrderByLastMessageAtDesc(request.getGuestId()).orElse(null);
        }
        return null;
    }
}
