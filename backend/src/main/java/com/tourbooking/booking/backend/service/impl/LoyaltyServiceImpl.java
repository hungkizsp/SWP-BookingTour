package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.entity.LoyaltyPoint;
import com.tourbooking.booking.backend.repository.LoyaltyPointRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.LoyaltyService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final UserRepository userRepository;

    @Override
    public LoyaltyPoint getPoint(Long userId) {
        LoyaltyPoint lp = loyaltyPointRepository.findByUserId(userId);
        if (lp != null) {
            return lp;
        }

        com.tourbooking.booking.backend.model.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        LoyaltyPoint created = new LoyaltyPoint();
        created.setUser(user);
        created.setPoints(0);
        return loyaltyPointRepository.save(created);
    }

    @Override
    public void addPoint(Long userId, int point) {
        LoyaltyPoint lp = getPoint(userId);
        if (lp == null) {
            return;
        }

        int current = lp.getPoints() == null ? 0 : lp.getPoints();
        lp.setPoints(current + Math.max(point, 0));
        loyaltyPointRepository.save(lp);
    }
}