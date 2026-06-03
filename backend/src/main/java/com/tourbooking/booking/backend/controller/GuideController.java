package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.service.GuideService;
import com.tourbooking.booking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/v1/guides")
@RequiredArgsConstructor
public class GuideController {

        private final GuideService guideService;
        private final UserService userService;

        @GetMapping("/assigned-tours")
        @PreAuthorize("hasRole('GUIDE')")
        public ApiResponse<List<TourScheduleResponse>> getAssignedTours(Authentication authentication) {
                try {
                        log.info("API Hit: /api/v1/guides/assigned-tours by user: {}", authentication.getName());
                        String email = authentication.getName();
                        UserResponse user = userService.getUserByEmail(email);
                        Long guideId = user.getId();

                        List<TourScheduleResponse> assignedTours = guideService.getAssignedTours(guideId);
                        log.info("API Success: Returning {} tours", assignedTours.size());

                        return ApiResponse.<List<TourScheduleResponse>>builder()
                                        .code(HttpStatus.OK.value())
                                        .message("Successfully retrieved assigned tours")
                                        .data(assignedTours)
                                        .build();
                } catch (Exception e) {
                        log.error("API Error in /assigned-tours: {}", e.getMessage(), e);
                        return ApiResponse.<List<TourScheduleResponse>>builder()
                                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .message("Error: " + e.getMessage())
                                        .data(null)
                                        .build();
                }
        }

        @GetMapping("/tours/{scheduleId}")
        @PreAuthorize("hasRole('GUIDE')")
        public ApiResponse<TourScheduleResponse> getTourDetails(
                        @PathVariable Long scheduleId,
                        Authentication authentication) {
                try {
                        String email = authentication.getName();
                        UserResponse user = userService.getUserByEmail(email);
                        TourScheduleResponse details = guideService.getAssignedTourDetails(user.getId(), scheduleId);
                        return ApiResponse.<TourScheduleResponse>builder()
                                        .code(HttpStatus.OK.value())
                                        .message("Successfully retrieved tour details")
                                        .data(details)
                                        .build();
                } catch (Exception e) {
                        log.error("Error in /tours/{}: {}", scheduleId, e.getMessage());
                        return ApiResponse.<TourScheduleResponse>builder()
                                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .message("Error: " + e.getMessage())
                                        .build();
                }
        }

        @PatchMapping("/tours/{scheduleId}/progress")
        @PreAuthorize("hasRole('GUIDE')")
        public ApiResponse<String> updateProgress(
                        @PathVariable Long scheduleId,
                        @RequestParam String progress,
                        Principal principal) {
                UserResponse user = userService.getUserByEmail(principal.getName());
                guideService.updateTourProgress(user.getId(), scheduleId, progress);
                return ApiResponse.<String>builder()
                                .code(HttpStatus.OK.value())
                                .message("Tour progress updated successfully")
                                .data(null)
                                .build();
        }

        @PostMapping("/tours/{scheduleId}/photos")
        @PreAuthorize("hasRole('GUIDE')")
        public ApiResponse<String> uploadPhotos(
                        @PathVariable Long scheduleId,
                        @RequestPart("photos") List<MultipartFile> photos,
                        Principal principal) {
                UserResponse user = userService.getUserByEmail(principal.getName());
                guideService.uploadTourPhotos(user.getId(), scheduleId, photos);
                return ApiResponse.<String>builder()
                                .code(HttpStatus.OK.value())
                                .message("Tour photos uploaded successfully")
                                .data(null)
                                .build();
        }

        @PostMapping("/tours/{scheduleId}/report")
        @PreAuthorize("hasRole('GUIDE')")
        public ApiResponse<String> submitReport(
                        @PathVariable Long scheduleId,
                        @RequestParam String content,
                        Principal principal) {
                UserResponse user = userService.getUserByEmail(principal.getName());
                guideService.submitTourReport(user.getId(), scheduleId, content);
                return ApiResponse.<String>builder()
                                .code(HttpStatus.OK.value())
                                .message("Tour report submitted successfully")
                                .data(null)
                                .build();
        }
}
