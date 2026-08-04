package com.tourbooking.booking.backend.ai.orchestrator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    private String agentName;
    private boolean success;
    private String textOutput;
    private Map<String, Object> data;
    private int tokensUsed;
    private long latencyMs;
    private String errorMessage;
}
