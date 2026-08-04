package com.tourbooking.booking.backend.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PromptManagerImpl implements PromptManager {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @Override
    public String getTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, this::loadTemplateFromResource);
    }

    @Override
    public String buildPrompt(String templateName, Map<String, Object> variables) {
        String template = getTemplate(templateName);
        if (template == null || template.isBlank()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, val);
        }
        return result;
    }

    private String loadTemplateFromResource(String templateName) {
        String resourcePath = "ai/prompts/" + templateName + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("[PromptManager] Template file not found at classpath:{}, fallback to empty prompt", resourcePath);
            return "";
        }
    }
}
