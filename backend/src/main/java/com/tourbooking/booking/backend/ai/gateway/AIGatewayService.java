package com.tourbooking.booking.backend.ai.gateway;

import java.util.Map;

/**
 * AI Gateway Service - Cổng giao tiếp chính với LLM (Gemini).
 * Tất cả Agent đều gọi qua đây, không gọi trực tiếp API.
 */
public interface AIGatewayService {

    /** Gọi LLM với system prompt + user message, trả về text */
    String generate(String systemPrompt, String userMessage);

    /** Gọi LLM với structured JSON output */
    <T> T generateStructured(String systemPrompt, String userMessage, Class<T> responseType);

    /** Gọi LLM với custom config (temperature, maxTokens...) */
    String generate(String systemPrompt, String userMessage, Map<String, Object> config);
}
