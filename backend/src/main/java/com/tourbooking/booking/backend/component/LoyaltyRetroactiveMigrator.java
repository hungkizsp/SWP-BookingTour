package com.tourbooking.booking.backend.component;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.LoyaltyPoint;
import com.tourbooking.booking.backend.model.entity.LoyaltyTransaction;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.LoyaltyPointRepository;
import com.tourbooking.booking.backend.repository.LoyaltyTransactionRepository;
import com.tourbooking.booking.backend.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyRetroactiveMigrator {

    private final BookingRepository bookingRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final LoyaltyService loyaltyService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateLoyaltyPoints() {
        log.info("[LoyaltyMigration] Starting Retroactive Loyalty Points Migration...");

        List<Booking> bookings = bookingRepository.findAll();
        int processedCount = 0;

        for (Booking booking : bookings) {
            BookingStatus status = booking.getStatus();
            // Check if status represents a successful or completed booking
            if (status == BookingStatus.CONFIRMED || status == BookingStatus.SUCCESS || status == BookingStatus.PAID || status == BookingStatus.COMPLETED) {
                if (booking.getUser() == null) {
                    continue;
                }

                BigDecimal paidAmount = BigDecimal.ZERO;
                if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.SUCCESS) {
                    paidAmount = booking.getPayment().getAmount();
                } else {
                    paidAmount = booking.getTotalPrice();
                }

                if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Points are calculated as 10% of purchased amount (1 point per 10,000 VND)
                int correctPoints = paidAmount.intValue() / 10000;
                if (correctPoints <= 0) {
                    continue;
                }

                LoyaltyTransaction existingTx = loyaltyTransactionRepository.findByBookingIdAndTransactionType(booking.getId(), "EARN").orElse(null);
                LoyaltyPoint lp = loyaltyService.getPoint(booking.getUser().getId());
                if (lp == null) {
                    continue;
                }

                int currentPoints = lp.getPoints() == null ? 0 : lp.getPoints();

                if (existingTx != null) {
                    int oldPoints = existingTx.getPoints();
                    if (oldPoints != correctPoints) {
                        int diff = correctPoints - oldPoints;
                        lp.setPoints(currentPoints + diff);
                        loyaltyPointRepository.save(lp);

                        existingTx.setPoints(correctPoints);
                        existingTx.setDescription("Cộng 10% điểm tích lũy cho booking #" + booking.getId());
                        loyaltyTransactionRepository.save(existingTx);

                        log.info("[LoyaltyMigration] Updated booking #{} loyalty points from {} to {} (User: {})", 
                                booking.getId(), oldPoints, correctPoints, booking.getUser().getEmail());
                        processedCount++;
                    }
                } else {
                    lp.setPoints(currentPoints + correctPoints);
                    loyaltyPointRepository.save(lp);

                    LoyaltyTransaction newTx = new LoyaltyTransaction();
                    newTx.setUser(lp.getUser());
                    newTx.setPoints(correctPoints);
                    newTx.setTransactionType("EARN");
                    newTx.setBooking(booking);
                    newTx.setDescription("Cộng 10% điểm tích lũy cho booking #" + booking.getId());
                    newTx.setCreatedAt(LocalDateTime.now());
                    loyaltyTransactionRepository.save(newTx);

                    log.info("[LoyaltyMigration] Created new retroactive 10% loyalty transaction for booking #{} with {} points (User: {})", 
                            booking.getId(), correctPoints, booking.getUser().getEmail());
                    processedCount++;
                }
            }
        }

        log.info("[LoyaltyMigration] Retroactive Loyalty Points Migration finished. Processed/updated {} bookings.", processedCount);
    }
}
