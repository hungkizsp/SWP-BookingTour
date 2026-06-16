package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.PagedResponse;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/wishlist", "/api/wishlist"})
@RequiredArgsConstructor
@CrossOrigin("*")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @RequestParam Long userId,
            @RequestParam Long tourId) {
        boolean isAdded = wishlistService.toggleWishlist(userId, tourId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "isAdded", isAdded,
                "message", isAdded ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích"
        ));
    }

    @GetMapping
    public ResponseEntity<Page<TourDetailResponse>> getWishlist(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long resolvedUserId = userId != null ? userId : currentUserId(authentication);
        return ResponseEntity.ok(wishlistService.getUserWishlist(resolvedUserId, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<PagedResponse<TourDetailResponse>> getMyWishlist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = currentUserId(authentication);
        Page<TourDetailResponse> wishlist = wishlistService.getUserWishlist(userId, page, size);
        PagedResponse<TourDetailResponse> response = PagedResponse.<TourDetailResponse>builder()
                .content(wishlist.getContent())
                .page(wishlist.getNumber())
                .size(wishlist.getSize())
                .totalElements(wishlist.getTotalElements())
                .totalPages(wishlist.getTotalPages())
                .first(wishlist.isFirst())
                .last(wishlist.isLast())
                .build();
        return ApiResponse.<PagedResponse<TourDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Wishlist retrieved")
                .data(response)
                .build();
    }

    @PostMapping("/{tourId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TourDetailResponse> addToWishlist(
            @PathVariable Long tourId,
            Authentication authentication) {
        TourDetailResponse tour = wishlistService.addWishlist(currentUserId(authentication), tourId);
        return ApiResponse.<TourDetailResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tour added to wishlist")
                .data(tour)
                .build();
    }

    @DeleteMapping("/{tourId}")
    public ApiResponse<Void> removeFromWishlist(
            @PathVariable Long tourId,
            Authentication authentication) {
        wishlistService.removeWishlist(currentUserId(authentication), tourId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Tour removed from wishlist")
                .build();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new com.tourbooking.booking.backend.exception.AppException(com.tourbooking.booking.backend.exception.ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tourbooking.booking.backend.exception.AppException(com.tourbooking.booking.backend.exception.ErrorCode.USER_NOT_FOUND))
                .getId();
    }
}
