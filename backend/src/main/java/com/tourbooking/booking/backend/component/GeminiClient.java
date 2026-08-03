package com.tourbooking.booking.backend.component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final RestTemplate restTemplateSlow;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .messageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
        this.restTemplateSlow = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .messageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String callGeminiRaw(String systemPrompt, String userMessage) throws Exception {
        String combinedPrompt = systemPrompt + (userMessage != null && !userMessage.isBlank() ? "\n\nTHÔNG TIN TOUR & ĐẦU VÀO:\n" + userMessage : "");

        java.util.Map<String, Object> part = java.util.Map.of("text", combinedPrompt);
        java.util.Map<String, Object> content = java.util.Map.of("parts", java.util.List.of(part));
        java.util.Map<String, Object> genConfig = java.util.Map.of(
                "temperature", 0.5,
                "maxOutputTokens", 2048,
                "topP", 0.9
        );
        java.util.Map<String, Object> requestMap = java.util.Map.of(
                "contents", java.util.List.of(content),
                "generationConfig", genConfig
        );

        String jsonBody = objectMapper.writeValueAsString(requestMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", geminiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        int maxRetries = 4;
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode text = root
                            .path("candidates").get(0)
                            .path("content")
                            .path("parts").get(0)
                            .path("text");

                    if (!text.isMissingNode()) {
                        return text.asText();
                    }
                }

                log.warn("[Gemini] Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Gemini API returned unexpected status: " + response.getStatusCode());

            } catch (org.springframework.web.client.RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 429) {
                    log.warn("[GeminiClient] Rate limit hit (429) on attempt {}. Sleeping 15s before retry...", attempt);
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    Thread.sleep(15000);
                } else if (status == 503) {
                    log.warn("[GeminiClient] Service unavailable (503) on attempt {}. Sleeping 3s before retry...", attempt);
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    Thread.sleep(3000);
                } else {
                    throw e;
                }
            }
        }
    }

    public String callGeminiCompareRaw(String systemPrompt, String userMessage) throws Exception {
        String jsonBody = "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + systemPrompt + "\"}]"
                + "}],"
                + "\"generationConfig\":{"
                + "\"temperature\":0.5,"
                + "\"maxOutputTokens\":8192,"
                + "\"topP\":0.9"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", geminiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        int maxRetries = 4;
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                ResponseEntity<String> response = restTemplateSlow.postForEntity(geminiApiUrl, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidate = root.path("candidates").get(0);
                    String finishReason = candidate.path("finishReason").asText("UNKNOWN");
                    JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");

                    if (!textNode.isMissingNode()) {
                        String text = textNode.asText();
                        if ("MAX_TOKENS".equals(finishReason)) {
                            log.warn("[Gemini Compare] Response was cut off due to MAX_TOKENS.");
                            text += "\n\n*(Nội dung đã được rút gọn do quá dài)*";
                        } else if (!"STOP".equals(finishReason) && !"UNKNOWN".equals(finishReason)) {
                            log.error("[Gemini Compare] Blocked or failed due to finishReason: {}", finishReason);
                            throw new RuntimeException("Gemini API blocked response: " + finishReason);
                        }
                        return text;
                    }
                }

                log.warn("[Gemini Compare] Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Gemini API returned unexpected status: " + response.getStatusCode());

            } catch (org.springframework.web.client.RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 429) {
                    log.warn("[GeminiCompareClient] Rate limit hit (429) on attempt {}. Sleeping 15s before retry...", attempt);
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    Thread.sleep(15000);
                } else if (status == 503) {
                    log.warn("[GeminiCompareClient] Service unavailable (503) on attempt {}. Sleeping 3s before retry...", attempt);
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    Thread.sleep(3000);
                } else {
                    throw e;
                }
            }
        }
    }
}
