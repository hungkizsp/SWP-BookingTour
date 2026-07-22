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

    private final com.tourbooking.booking.backend.repository.LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final com.tourbooking.booking.backend.repository.BookingRepository bookingRepository;

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
        if (lp == null || point <= 0) {
            return;
        }

        int current = lp.getPoints() == null ? 0 : lp.getPoints();
        lp.setPoints(current + point);
        loyaltyPointRepository.save(lp);
        
        com.tourbooking.booking.backend.model.entity.LoyaltyTransaction tx = new com.tourbooking.booking.backend.model.entity.LoyaltyTransaction();
        tx.setUser(lp.getUser());
        tx.setPoints(point);
        tx.setTransactionType("EARN");
        tx.setDescription("Cộng " + point + " điểm tích lũy");
        tx.setCreatedAt(java.time.LocalDateTime.now());
        loyaltyTransactionRepository.save(tx);
    }

    @Override
    public com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse getMyPoints(Long userId) {
        LoyaltyPoint lp = getPoint(userId);
        int points = lp != null && lp.getPoints() != null ? lp.getPoints() : 0;
        
        java.util.List<com.tourbooking.booking.backend.model.entity.LoyaltyTransaction> txList = loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        java.util.List<com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse.LoyaltyTransactionDto> dtoList = txList.stream().map(t -> 
            com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse.LoyaltyTransactionDto.builder()
                .transactionType(t.getTransactionType())
                .points(t.getPoints())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt().toString())
                .build()
        ).toList();
        
        return com.tourbooking.booking.backend.model.dto.response.LoyaltyPointResponse.builder()
                .totalPoints(points)
                .pointsValue(java.math.BigDecimal.valueOf(points * 1000L))
                .transactions(dtoList)
                .build();
    }

    @Override
    public com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse validateRedeem(Long userId, com.tourbooking.booking.backend.model.dto.request.LoyaltyRedeemRequest request) {
        LoyaltyPoint lp = getPoint(userId);
        int currentPoints = lp != null && lp.getPoints() != null ? lp.getPoints() : 0;
        
        if (request.getPointsToRedeem() <= 0) {
            return com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse.builder()
                    .valid(false)
                    .message("Số điểm không hợp lệ")
                    .build();
        }
        
        if (currentPoints < request.getPointsToRedeem()) {
            return com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse.builder()
                    .valid(false)
                    .message("Không đủ điểm tích lũy")
                    .build();
        }
        
        java.math.BigDecimal discountAmount = java.math.BigDecimal.valueOf(request.getPointsToRedeem() * 1000L);
        java.math.BigDecimal maxDiscount = request.getBookingTotal().multiply(new java.math.BigDecimal("0.3")); // Max 30%
        
        if (discountAmount.compareTo(maxDiscount) > 0) {
            return com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse.builder()
                    .valid(false)
                    .message("Chỉ được dùng điểm thanh toán tối đa 30% giá trị đơn hàng (" + maxDiscount.longValue() + "đ)")
                    .build();
        }
        
        java.math.BigDecimal finalTotal = request.getBookingTotal().subtract(discountAmount);
        
        return com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse.builder()
                .valid(true)
                .discountAmount(discountAmount)
                .remainingPoints(currentPoints - request.getPointsToRedeem())
                .finalTotal(finalTotal.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : finalTotal)
                .message("Áp dụng điểm thành công (-" + discountAmount.longValue() + "đ)")
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse redeem(Long userId, com.tourbooking.booking.backend.model.dto.request.LoyaltyRedeemRequest request) {
        com.tourbooking.booking.backend.model.dto.response.LoyaltyRedeemResponse validateRes = validateRedeem(userId, request);
        if (!validateRes.isValid()) {
            throw new com.tourbooking.booking.backend.exception.AppException(com.tourbooking.booking.backend.exception.ErrorCode.INVALID_REQUEST, validateRes.getMessage());
        }
        
        LoyaltyPoint lp = getPoint(userId);
        lp.setPoints(lp.getPoints() - request.getPointsToRedeem());
        loyaltyPointRepository.save(lp);
        
        com.tourbooking.booking.backend.model.entity.LoyaltyTransaction tx = new com.tourbooking.booking.backend.model.entity.LoyaltyTransaction();
        tx.setUser(lp.getUser());
        tx.setPoints(request.getPointsToRedeem());
        tx.setTransactionType("REDEEM");
        tx.setDescription("Đổi " + request.getPointsToRedeem() + " điểm thanh toán booking");
        
        if (request.getBookingId() != null) {
            com.tourbooking.booking.backend.model.entity.Booking booking = bookingRepository.findById(request.getBookingId()).orElse(null);
            tx.setBooking(booking);
            if (booking != null) {
                booking.setLoyaltyPointsUsed(request.getPointsToRedeem());
                booking.setLoyaltyDiscountAmount(validateRes.getDiscountAmount());
                bookingRepository.save(booking);
            }
        }
        tx.setCreatedAt(java.time.LocalDateTime.now());
        loyaltyTransactionRepository.save(tx);
        
        return validateRes;
    }
}