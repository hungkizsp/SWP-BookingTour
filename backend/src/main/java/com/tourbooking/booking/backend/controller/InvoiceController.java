package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.Invoice;
import com.tourbooking.booking.backend.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

//@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{bookingId}")
    public Invoice getInvoice(@PathVariable Long bookingId) {

        return invoiceService.getInvoice(bookingId);
    }
}