package com.tourbooking.booking.backend.component;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.Payment;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDataSeeder {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedPayments() {
        log.info("Starting Payment Data Seeding...");
        
        List<Booking> bookings = bookingRepository.findAll();
        int updated = 0;
        int seeded = 0;

        for (Booking booking : bookings) {
            // 1. If status is PENDING/CONFIRMED but has no payment, let's treat it as SUCCESS for the report demo
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.SUCCESS);
                bookingRepository.save(booking);
                updated++;
            }

            // 2. Create payment if not exists
            if (booking.getPayment() == null) {
                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO);
                payment.setPaymentMethod("CASH");
                payment.setTransactionCode("SEED-" + booking.getId());
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus(PaymentStatus.SUCCESS);
                
                paymentRepository.save(payment);
                seeded++;
            }
        }

        log.info("Seeding completed. Updated {} bookings to SUCCESS, created {} payments.", updated, seeded);
    }
}
