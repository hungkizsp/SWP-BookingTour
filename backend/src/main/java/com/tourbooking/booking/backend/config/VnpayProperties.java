package com.tourbooking.booking.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    private String tmnCode;
    private String hashSecret;
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:3000/pages/client/vnpay-return.html";
    private String version = "2.1.0";
    private String command = "pay";

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode != null ? tmnCode.trim() : null;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret != null ? hashSecret.trim() : null;
    }
}
