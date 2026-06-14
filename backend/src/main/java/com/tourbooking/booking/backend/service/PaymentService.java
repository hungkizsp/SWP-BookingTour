package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;
import com.tourbooking.booking.backend.model.dto.response.VNPayConfirmResponse;

import java.time.LocalDate;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {

    PaymentResponse makePayment(PaymentRequest request);

    PaymentResponse createPayOSPayment(PaymentRequest request);
    
    PaymentResponse createCashPaymentIntent(PaymentRequest request);

    PaymentResponse createVNPayPayment(Long bookingId, HttpServletRequest request);

    VNPayConfirmResponse confirmVNPayReturn(Map<String, String> params);

    void handlePayOSWebhook(String rawPayload, String signature);

    PaymentResponse confirmPayOsAfterReturn(long orderCode);

    PaymentResponse confirmManualPayment(PaymentRequest request);

    /**
     * Đối soát chủ động các payment PayOS PENDING trong khoảng ngày (trước khi lập báo cáo).
     * @return số bản ghi được cập nhật thành SUCCESS
     */
    int reconcilePendingPayOsPaymentsInRange(LocalDate startDate, LocalDate endDate);
}