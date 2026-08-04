package com.tourbooking.booking.backend.ai.orchestrator;

import java.util.List;

public interface AIOrchestrator {

    /** Execute a given workflow with an ordered list of agents */
    AgentContext runWorkflow(AIWorkflow workflow, AgentContext context, List<Agent> agents);
}
