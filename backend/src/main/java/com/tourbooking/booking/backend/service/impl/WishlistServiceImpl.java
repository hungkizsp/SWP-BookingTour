package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.mapper.TourMapper;
import com.tourbooking.booking.backend.model.dto.response.TourDetailResponse;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.Wishlist;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.repository.WishlistRepository;
import com.tourbooking.booking.backend.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long tourId) {
        requireUser(userId);
        requireTour(tourId);

        java.util.Optional<Wishlist> existing = wishlistRepository.findByUserIdAndTourId(userId, tourId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // Removed
        } else {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(userRepository.getReferenceById(userId));
            wishlist.setTour(tourRepository.getReferenceById(tourId));
            wishlistRepository.save(wishlist);
            return true; // Added
        }
    }

    @Override
    @Transactional
    public TourDetailResponse addWishlist(Long userId, Long tourId) {
        User user = requireUser(userId);
        Tour tour = requireTour(tourId);

        Wishlist wishlist = wishlistRepository.findByUserIdAndTourId(userId, tourId)
                .orElseGet(() -> {
                    Wishlist item = new Wishlist();
                    item.setUser(user);
                    item.setTour(tour);
                    return wishlistRepository.save(item);
                });
        return TourMapper.toDetailResponse(wishlist.getTour());
    }

    @Override
    @Transactional
    public void removeWishlist(Long userId, Long tourId) {
        requireUser(userId);
        requireTour(tourId);
        wishlistRepository.findByUserIdAndTourId(userId, tourId)
                .ifPresent(wishlistRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourDetailResponse> getUserWishlist(Long userId, int page, int size) {
        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(userId, PageRequest.of(page, size));
        return wishlistPage.map(w -> TourMapper.toDetailResponse(w.getTour()));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Tour requireTour(Long tourId) {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
    }
}
