package com.tourbooking.booking.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourbooking.booking.backend.config.PayOSProperties;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * PayOS Service - Complete implementation synced from nested project
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOSProperties payOSProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String createPaymentLink(Long orderCode, int amount, String description) {
        if (!StringUtils.hasText(payOSProperties.getClientId()) ||
                !StringUtils.hasText(payOSProperties.getApiKey()) ||
                !StringUtils.hasText(payOSProperties.getChecksumKey())) {
            throw new AppException(ErrorCode.PAYOS_NOT_CONFIGURED, "Thiếu cấu hình PayOS");
        }

        if (orderCode == null || orderCode <= 0) {
            throw new AppException(ErrorCode.PAYOS_LINK_FAILED, "orderCode không hợp lệ");
        }
        if (amount < 1000) {
            throw new AppException(ErrorCode.PAYOS_LINK_FAILED, "Số tiền tối thiểu là 1.000 VND");
        }

        description = StringUtils.hasText(description) ? description.trim() : "Thanh toán booking " + orderCode;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("returnUrl", payOSProperties.getReturnUrl());
        body.put("cancelUrl", payOSProperties.getCancelUrl());

        String signature = createPaymentRequestSignature(body);
        body.put("signature", signature);

        // Thay thế phần log cũ trong createPaymentLink
        log.info("=== [PAYOS] CREATE PAYMENT LINK ===");
        log.info("orderCode   : {}", orderCode);
        log.info("amount      : {}", amount);
        log.info("description : {}", description);
        log.info("returnUrl   : {}", payOSProperties.getReturnUrl());
        log.info("cancelUrl   : {}", payOSProperties.getCancelUrl());
        log.info("=== PAYOS ENDPOINT CHECK ===");
        log.info("BaseUrl from properties: [{}]", payOSProperties.getBaseUrl());
        log.info("Full endpoint being called: [{}]", payOSProperties.getBaseUrl() + "/v2/payment-requests");

        try {
            log.info("Full request body: {}", objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSProperties.getClientId().trim());
        headers.set("x-api-key", payOSProperties.getApiKey().trim());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            String baseUrl = payOSProperties.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String endpoint = baseUrl + "/v2/payment-requests";
            
            log.info("Final API Endpoint: {}", endpoint);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"00".equals(root.path("code").asText())) {
                String desc = root.path("desc").asText("Unknown error");
                log.error("PayOS từ chối: code={} desc={}", root.path("code").asText(), desc);
                throw new AppException(ErrorCode.PAYOS_LINK_FAILED, desc);
            }

            String checkoutUrl = root.path("data").path("checkoutUrl").asText().trim();
            log.info("Creating PayOS link success: {}", checkoutUrl);
            return checkoutUrl;

        } catch (HttpClientErrorException e) {
            String respBody = e.getResponseBodyAsString();
            log.error("=== PAYOS 400 BAD REQUEST ===");
            log.error("Response: {}", respBody);
            throw new AppException(ErrorCode.PAYOS_LINK_FAILED, "PayOS lỗi: " + respBody);
        } catch (Exception e) {
            log.error("Error calling PayOS", e);
            throw new AppException(ErrorCode.PAYOS_LINK_FAILED);
        }
    }

    /** Signature ĐÚNG CHUẨN PayOS - Sử dụng TreeMap để sort alphabet */
    /**
     * TẠO SIGNATURE ĐÚNG CHUẨN PAYOS
     * Sử dụng TreeMap để tự động sắp xếp key theo alphabet (bắt buộc)
     */
    private String createPaymentRequestSignature(Map<String, Object> body) {
        try {
            // TreeMap sẽ tự sort key: amount → cancelUrl → description → orderCode →
            // returnUrl
            TreeMap<String, Object> sorted = new TreeMap<>(body);

            StringBuilder payload = new StringBuilder(256);
            boolean first = true;

            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) {
                    payload.append("&");
                }
                payload.append(entry.getKey())
                        .append("=")
                        .append(stringValue(entry.getValue()));
                first = false;
            }

            String payloadStr = payload.toString();

            // Debug cực kỳ quan trọng
            log.info("=== PAYOS SIGNATURE PAYLOAD ===");
            log.info("Payload: {}", payloadStr);
            log.info("Checksum Key length: {}", payOSProperties.getChecksumKey().length());

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    payOSProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            byte[] hash = mac.doFinal(payloadStr.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(hash);

            log.info("Generated Signature: {}", signature);

            return signature;

        } catch (Exception e) {
            log.error("Tạo signature PayOS thất bại", e);
            throw new AppException(ErrorCode.PAYOS_LINK_FAILED, "Không thể tạo chữ ký thanh toán");
        }
    }

    private String stringValue(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    public boolean verifyPayOsDataSignature(JsonNode dataNode, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank() ||
                dataNode == null || dataNode.isNull() || !dataNode.isObject()) {
            return false;
        }
        String checksumKey = payOSProperties.getChecksumKey();
        if (!StringUtils.hasText(checksumKey))
            return false;

        try {
            String payload = buildSignatureQueryString(dataNode);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String hex = bytesToHex(hash);
            return hex.equalsIgnoreCase(providedSignature.trim());
        } catch (Exception e) {
            log.warn("Verify signature error", e);
            return false;
        }
    }

    public Optional<JsonNode> fetchPaymentRequestByOrderCode(long orderCode) {
        if (!StringUtils.hasText(payOSProperties.getClientId()) ||
                !StringUtils.hasText(payOSProperties.getApiKey())) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", payOSProperties.getClientId());
        headers.set("x-api-key", payOSProperties.getApiKey());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    payOSProperties.getBaseUrl() + "/v2/payment-requests/" + orderCode,
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"00".equals(root.path("code").asText())) {
                return Optional.empty();
            }

            JsonNode data = root.path("data");
            String sig = root.path("signature").asText("");
            if (!sig.isBlank() && !verifyPayOsDataSignature(data, sig)) {
                return Optional.empty();
            }
            return Optional.of(data);
        } catch (Exception e) {
            log.warn("Fetch payment request error", e);
            return Optional.empty();
        }
    }

    public static boolean amountsMatch(BigDecimal paymentAmount, JsonNode payOsData) {
        if (paymentAmount == null || payOsData == null || !payOsData.has("amount")) {
            return true;
        }
        long remote = payOsData.path("amount").asLong(-1);
        if (remote < 0)
            return true;
        return paymentAmount.compareTo(BigDecimal.valueOf(remote)) == 0;
    }

    private String buildSignatureQueryString(JsonNode data) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        data.fields().forEachRemaining(e -> sorted.put(e.getKey(), e.getValue()));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JsonNode> e : sorted.entrySet()) {
            if (!sb.isEmpty())
                sb.append('&');
            sb.append(e.getKey()).append('=').append(serializeSignatureValue(e.getValue()));
        }
        return sb.toString();
    }

    private String serializeSignatureValue(JsonNode val) {
        if (val == null || val.isNull())
            return "";
        if (val.isBoolean())
            return val.booleanValue() ? "true" : "false";
        if (val.isIntegralNumber())
            return Long.toString(val.longValue());
        if (val.isNumber())
            return val.decimalValue().stripTrailingZeros().toPlainString();
        if (val.isTextual())
            return val.asText();
        if (val.isArray())
            return serializeArrayForSignature((ArrayNode) val);
        if (val.isObject()) {
            try {
                return objectMapper.writeValueAsString(sortKeysObjectNode(val));
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private String serializeArrayForSignature(ArrayNode arr) {
        try {
            ArrayNode out = objectMapper.createArrayNode();
            for (JsonNode item : arr) {
                if (item != null && item.isObject()) {
                    out.add(sortKeysObjectNode(item));
                } else {
                    out.add(item);
                }
            }
            return objectMapper.writeValueAsString(out);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ObjectNode sortKeysObjectNode(JsonNode obj) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        obj.fields().forEachRemaining(e -> sorted.put(e.getKey(), e.getValue()));
        ObjectNode out = objectMapper.createObjectNode();
        sorted.forEach(out::set);
        return out;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
