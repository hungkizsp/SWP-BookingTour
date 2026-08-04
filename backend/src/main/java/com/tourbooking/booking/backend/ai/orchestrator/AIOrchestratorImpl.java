package com.tourbooking.booking.backend.ai.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestratorImpl implements AIOrchestrator {

    private final AgentExecutor agentExecutor;

    @Override
    public AgentContext runWorkflow(AIWorkflow workflow, AgentContext context, List<Agent> agents) {
        if (context.getWorkflowId() == null) {
            context.setWorkflowId(UUID.randomUUID().toString());
        }
        context.setWorkflowType(workflow);

        log.info("[AIOrchestrator] Starting workflow {} (ID: {}) with {} agents", workflow, context.getWorkflowId(), agents != null ? agents.size() : 0);

        if (agents != null) {
            for (Agent agent : agents) {
                log.info("[AIOrchestrator] Executing agent {}", agent.getName());
                AgentResult result = agentExecutor.execute(agent, context);
                context.addResult(result);
            }
        }

        log.info("[AIOrchestrator] Workflow {} completed", workflow);
        return context;
    }
}
