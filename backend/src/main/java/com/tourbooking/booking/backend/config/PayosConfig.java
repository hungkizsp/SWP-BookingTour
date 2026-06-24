package com.tourbooking.booking.backend.config;

import vn.payos.PayOS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayosConfig {

    @Value("${PAYOS_CLIENT_ID:}")
    private String clientId;

    @Value("${PAYOS_API_KEY:}")
    private String apiKey;

    @Value("${PAYOS_CHECKSUM_KEY:}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
