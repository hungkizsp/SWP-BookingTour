package com.tourbooking.booking.backend.ai.prompt;

import java.util.Map;

public interface PromptManager {

    /** Load prompt template by name and inject variable values */
    String buildPrompt(String templateName, Map<String, Object> variables);

    /** Get raw template content by name */
    String getTemplate(String templateName);
}
