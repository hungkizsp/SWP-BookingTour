package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.LoyaltyPoint;

public interface LoyaltyService {

    LoyaltyPoint getPoint(Long userId);
    void addPoint(Long userId, int point);
    void addPoint(Long userId, int point, Long bookingId);
    
    com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse getMyPoints(Long userId);
    com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse validateRedeem(Long userId, com.tourbooking.booking.backend.model.dto.request.LoyaltyRedeemRequest request);
    com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse redeem(Long userId, com.tourbooking.booking.backend.model.dto.request.LoyaltyRedeemRequest request);
}