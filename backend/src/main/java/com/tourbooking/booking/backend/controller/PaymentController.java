package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;
import com.tourbooking.booking.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> makePayment(@RequestBody PaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .code(200)
                .message("Payment processed successfully")
                .data(paymentService.makePayment(request))
                .build();
    }

    @PostMapping("/payos/create")
    public ApiResponse<PaymentResponse> createPayOSPayment(@RequestBody PaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .code(200)
                .message("PayOS link created")
                .data(paymentService.createPayOSPayment(request))
                .build();
    }

    @PostMapping("/payos/webhook")
    public void handlePayOSWebhook(@RequestBody String payload,
                                   @RequestHeader(value = "x-api-validate-signature", required = false) String signature) {
        log.info("Received PayOS webhook");
        paymentService.handlePayOSWebhook(payload, signature);
    }

    @GetMapping("/payos/confirm/{orderCode}")
    public ApiResponse<PaymentResponse> confirmPayOs(@PathVariable long orderCode) {
        return ApiResponse.<PaymentResponse>builder()
                .code(200)
                .message("Payment status confirmed")
                .data(paymentService.confirmPayOsAfterReturn(orderCode))
                .build();
    }

    @PostMapping("/manual-confirm")
    public ApiResponse<PaymentResponse> confirmManual(@RequestBody PaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .code(200)
                .message("Manual payment confirmed")
                .data(paymentService.confirmManualPayment(request))
                .build();
    }
}