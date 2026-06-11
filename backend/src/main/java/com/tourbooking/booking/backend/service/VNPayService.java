package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.config.VNPayConfig;
import com.tourbooking.booking.backend.config.VnpayProperties;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private static final DateTimeFormatter VNP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int PAYMENT_EXPIRE_MINUTES = 15;

    private final VnpayProperties vnpayProperties;

    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String ipAddress) {
        ensureConfigured();

        LocalDateTime now = LocalDateTime.now();
        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", now.format(VNP_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(PAYMENT_EXPIRE_MINUTES).format(VNP_DATE_FORMAT));

        String hashData = VNPayConfig.buildHashData(params);
        String secureHash = VNPayConfig.hmacSHA512(vnpayProperties.getHashSecret(), hashData);
        String query = VNPayConfig.buildQuery(params);

        return vnpayProperties.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác minh chữ ký khi VNPay redirect về — 3 bước theo chuẩn VNPay 2.1.0.
     */
    public boolean validateReturn(Map<String, String> rawParams) {
        ensureConfigured();

        if (rawParams == null || rawParams.isEmpty()) {
            return false;
        }

        // Bước 1: TreeMap + loại bỏ vnp_SecureHash / vnp_SecureHashType
        TreeMap<String, String> params = new TreeMap<>(rawParams);
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        if (!StringUtils.hasText(receivedHash)) {
            return false;
        }

        // Bước 2: buildHashData + HMAC SHA-512
        String hashData = VNPayConfig.buildHashData(params);
        String calculatedHash = VNPayConfig.hmacSHA512(vnpayProperties.getHashSecret(), hashData);

        boolean isValid = calculatedHash.equalsIgnoreCase(receivedHash.trim());

        // Debug logs
        log.info("--- VNPAY VALIDATION DEBUG ---");
        log.info("Raw Params: {}", rawParams);
        log.info("Hash Data string: {}", hashData);
        log.info("Calculated Hash: {}", calculatedHash);
        log.info("Received Hash  : {}", receivedHash);
        log.info("Is Valid?      : {}", isValid);
        log.info("------------------------------");

        // Bước 3: so sánh không phân biệt hoa thường
        return isValid;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(vnpayProperties.getTmnCode())
                || !StringUtils.hasText(vnpayProperties.getHashSecret())) {
            throw new AppException(ErrorCode.VNPAY_NOT_CONFIGURED);
        }
    }
}
