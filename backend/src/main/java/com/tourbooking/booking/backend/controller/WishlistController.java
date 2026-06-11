package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@CrossOrigin("*")
public class WishlistController {

    private final WishlistService wishlistService;

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
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(wishlistService.getUserWishlist(userId, page, size));
    }
}
