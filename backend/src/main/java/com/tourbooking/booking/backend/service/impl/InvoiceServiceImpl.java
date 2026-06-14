package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.Invoice;

import com.tourbooking.booking.backend.repository.BookingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.tourbooking.booking.backend.repository.InvoiceRepository;
import com.tourbooking.booking.backend.service.InvoiceService;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;

    @Override
    public Invoice getInvoice(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
                
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
            boolean isStaffOrAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            if (!isStaffOrAdmin) {
                if (!booking.getUser().getEmail().equals(email)) {
                    throw new RuntimeException("Access denied");
                }
            }
        }

        return invoiceRepository.findByBookingId(bookingId);
    }

    @Override
    public Invoice generateInvoice(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow();

        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());

        return invoiceRepository.save(invoice);
    }
}