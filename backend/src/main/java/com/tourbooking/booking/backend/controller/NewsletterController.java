package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.NewsletterRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.NewsletterResponse;
import com.tourbooking.booking.backend.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/newsletters")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @GetMapping
    public ApiResponse<List<NewsletterResponse>> getAllNewsletters() {
        return ApiResponse.<List<NewsletterResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved all newsletter subscriptions")
                .data(newsletterService.getAllNewsletters())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsletterResponse> getNewsletterById(@PathVariable Long id) {
        return ApiResponse.<NewsletterResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved newsletter subscription details")
                .data(newsletterService.getNewsletterById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NewsletterResponse> createNewsletter(@RequestBody NewsletterRequest request) {
        return ApiResponse.<NewsletterResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Newsletter subscription created successfully")
                .data(newsletterService.createNewsletter(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsletterResponse> updateNewsletter(@PathVariable Long id,
            @RequestBody NewsletterRequest request) {
        return ApiResponse.<NewsletterResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Newsletter subscription updated successfully")
                .data(newsletterService.updateNewsletter(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNewsletter(@PathVariable Long id) {
        newsletterService.deleteNewsletter(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Newsletter subscription deleted successfully")
                .build();
    }
}
