package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.ChangePasswordRequest;
import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/customers", "/api/v1/customers"})
@RequiredArgsConstructor
public class CustomerController {
    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Profile retrieved")
                .data(userService.getProfile(authentication.getName()))
                .build();
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(Authentication authentication, @RequestBody UserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Profile updated")
                .data(userService.updateProfile(authentication.getName(), request))
                .build();
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Password changed successfully")
                .build();
    }
}
