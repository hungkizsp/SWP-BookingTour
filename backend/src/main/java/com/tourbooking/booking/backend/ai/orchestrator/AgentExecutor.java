package com.tourbooking.booking.backend.ai.orchestrator;

import com.tourbooking.booking.backend.ai.logger.AILogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final AILogService aiLogService;

    public AgentResult execute(Agent agent, AgentContext context) {
        long startTime = System.currentTimeMillis();
        String agentName = agent.getName();
        log.info("[AgentExecutor] Starting agent: {}", agentName);

        try {
            AgentResult result = agent.execute(context);
            long latency = System.currentTimeMillis() - startTime;
            result.setLatencyMs(latency);

            aiLogService.log(agentName, context.getWorkflowId(), latency, result.isSuccess() ? "SUCCESS" : "FAILED", result.getErrorMessage());
            log.info("[AgentExecutor] Finished agent: {} in {}ms", agentName, latency);
            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("[AgentExecutor] Execution error in agent {}: {}", agentName, e.getMessage(), e);
            aiLogService.log(agentName, context.getWorkflowId(), latency, "ERROR", e.getMessage());

            return AgentResult.builder()
                    .agentName(agentName)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .latencyMs(latency)
                    .textOutput("Agent " + agentName + " encountered an unexpected error.")
                    .build();
        }
    }
}
