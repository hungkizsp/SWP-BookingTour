package com.tourbooking.booking.backend.config;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class VNPayConfig {

    private static final String FIELD_SECURE_HASH = "vnp_SecureHash";
    private static final String FIELD_SECURE_HASH_TYPE = "vnp_SecureHashType";

    private VNPayConfig() {
    }

    /**
     * Nối chuỗi hash: key=value theo thứ tự A-Z (TreeMap), encode cả key và value bằng US-ASCII.
     */
    public static String buildHashData(Map<String, String> fields) {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            if (FIELD_SECURE_HASH.equals(fieldName) || FIELD_SECURE_HASH_TYPE.equals(fieldName)) {
                continue;
            }
            String fieldValue = entry.getValue();
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if (!hashData.isEmpty()) {
                hashData.append('&');
            }
            hashData.append(encodeAscii(fieldName));
            hashData.append('=');
            hashData.append(encodeAscii(fieldValue));
        }
        return hashData.toString();
    }

    /**
     * Query string (chưa có vnp_SecureHash): encode cả key và value bằng US-ASCII, thứ tự A-Z.
     */
    public static String buildQuery(Map<String, String> fields) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            if (FIELD_SECURE_HASH.equals(fieldName) || FIELD_SECURE_HASH_TYPE.equals(fieldName)) {
                continue;
            }
            String fieldValue = entry.getValue();
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(encodeAscii(fieldName));
            query.append('=');
            query.append(encodeAscii(fieldValue));
        }
        return query.toString();
    }

    public static String hmacSHA512(String key, String data) {
        if (key == null || data == null) {
            throw new IllegalArgumentException("VNPay HMAC key and data must not be null");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            byte[] keyBytes = key.trim().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("VNPay HMAC-SHA512 failed", e);
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip != null ? ip : "127.0.0.1";
    }

    private static String encodeAscii(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (Exception e) {
            return value;
        }
    }
}
