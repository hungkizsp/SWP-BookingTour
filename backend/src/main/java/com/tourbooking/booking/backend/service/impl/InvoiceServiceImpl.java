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