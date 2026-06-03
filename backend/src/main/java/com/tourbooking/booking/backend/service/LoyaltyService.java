package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.LoyaltyPoint;

public interface LoyaltyService {

    LoyaltyPoint getPoint(Long userId);

    void addPoint(Long userId, int point);
}