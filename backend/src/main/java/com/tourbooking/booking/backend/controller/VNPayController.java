package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;
import com.tourbooking.booking.backend.model.dto.response.VNPayConfirmResponse;
import com.tourbooking.booking.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.TreeMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final PaymentService paymentService;

    @PostMapping("/create/{bookingId}")
    public ApiResponse<PaymentResponse> createVNPayPayment(
            @PathVariable Long bookingId,
            HttpServletRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .code(200)
                .message("VNPay checkout URL created")
                .data(paymentService.createVNPayPayment(bookingId, request))
                .build();
    }

    @GetMapping("/confirm")
    public ApiResponse<VNPayConfirmResponse> confirmVNPayReturn(HttpServletRequest request) {
        TreeMap<String, String> params = extractQueryParams(request);
        log.info("VNPay return confirm for txnRef={}", params.get("vnp_TxnRef"));
        return ApiResponse.<VNPayConfirmResponse>builder()
                .code(200)
                .message("VNPay return processed")
                .data(paymentService.confirmVNPayReturn(params))
                .build();
    }

    private static TreeMap<String, String> extractQueryParams(HttpServletRequest request) {
        TreeMap<String, String> params = new TreeMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0 && values[0] != null) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
