package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;

import java.time.LocalDate;

public interface PaymentService {

    PaymentResponse makePayment(PaymentRequest request);

    PaymentResponse createPayOSPayment(PaymentRequest request);

    void handlePayOSWebhook(String rawPayload, String signature);

    PaymentResponse confirmPayOsAfterReturn(long orderCode);

    PaymentResponse confirmManualPayment(PaymentRequest request);

    /**
     * Đối soát chủ động các payment PayOS PENDING trong khoảng ngày (trước khi lập báo cáo).
     * @return số bản ghi được cập nhật thành SUCCESS
     */
    int reconcilePendingPayOsPaymentsInRange(LocalDate startDate, LocalDate endDate);
}