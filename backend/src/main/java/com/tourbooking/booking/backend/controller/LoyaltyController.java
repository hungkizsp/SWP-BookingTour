package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.LoyaltyRedeemRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse;
import com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse;
import com.tourbooking.booking.backend.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    @GetMapping("/my-points")
    public ApiResponse<LoyaltyPointResponse> getMyPoints(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<LoyaltyPointResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin điểm thành công")
                .data(loyaltyService.getMyPoints(userId))
                .build();
    }

    @PostMapping("/validate-redeem")
    public ApiResponse<LoyaltyRedeemResponse> validateRedeem(@RequestBody LoyaltyRedeemRequest request, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<LoyaltyRedeemResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Kiểm tra điểm thành công")
                .data(loyaltyService.validateRedeem(userId, request))
                .build();
    }

    @PostMapping("/redeem")
    public ApiResponse<LoyaltyRedeemResponse> redeem(@RequestBody LoyaltyRedeemRequest request, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.<LoyaltyRedeemResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Đổi điểm thành công")
                .data(loyaltyService.redeem(userId, request))
                .build();
    }
}
