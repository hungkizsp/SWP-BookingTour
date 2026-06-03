package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.entity.Document;
import com.tourbooking.booking.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload-document")
    public ApiResponse<Document> uploadDoc(@RequestParam Long userId, @RequestParam String url,
            @RequestParam String type) {
        return ApiResponse.success(documentService.upload(userId, url, type));
    }
}