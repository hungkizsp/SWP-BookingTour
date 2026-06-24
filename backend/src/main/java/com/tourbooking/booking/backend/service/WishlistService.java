package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import org.springframework.data.domain.Page;

public interface WishlistService {
    boolean toggleWishlist(Long userId, Long tourId);
    Page<TourDetailResponse> getUserWishlist(Long userId, int page, int size);
}
