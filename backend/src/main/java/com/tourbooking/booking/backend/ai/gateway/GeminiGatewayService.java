package com.tourbooking.booking.backend.ai.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourbooking.booking.backend.component.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiGatewayService implements AIGatewayService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(String systemPrompt, String userMessage) {
        try {
            return geminiClient.callGeminiRaw(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("[AIGateway] Failed to call Gemini LLM: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối AI Gateway: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T generateStructured(String systemPrompt, String userMessage, Class<T> responseType) {
        String structuredPrompt = systemPrompt + "\n\nYÊU CẦU ĐỊNH DẠNG: Trả về kết quả dưới dạng JSON hợp lệ (không kèm theo bất kỳ văn bản giải thích nào khác ngoài JSON). Schema mong muốn của " + responseType.getSimpleName() + ".";
        String rawResponse = generate(structuredPrompt, userMessage);
        try {
            // Clean markdown block ```json ... ``` if present
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            return objectMapper.readValue(cleaned.trim(), responseType);
        } catch (Exception e) {
            log.error("[AIGateway] Failed to parse structured JSON into {}: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("Không thể giải mã kết quả JSON từ AI Gateway", e);
        }
    }

    @Override
    public String generate(String systemPrompt, String userMessage, Map<String, Object> config) {
        // Fallback to standard generate for now
        return generate(systemPrompt, userMessage);
    }
}
