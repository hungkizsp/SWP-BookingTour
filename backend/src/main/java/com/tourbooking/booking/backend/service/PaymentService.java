package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse makePayment(PaymentRequest request);

    PaymentResponse createPayOSPayment(PaymentRequest request);

    void handlePayOSWebhook(String rawPayload, String signature);

    PaymentResponse confirmPayOsAfterReturn(long orderCode);

    PaymentResponse confirmManualPayment(PaymentRequest request);
}