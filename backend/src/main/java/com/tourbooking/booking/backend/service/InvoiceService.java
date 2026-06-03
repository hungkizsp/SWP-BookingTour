package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.Invoice;

public interface InvoiceService {

    Invoice getInvoice(Long bookingId);

    Invoice generateInvoice(Long bookingId);
}