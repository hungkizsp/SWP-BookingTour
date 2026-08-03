package com.tourbooking.booking.backend.ai.orchestrator;

public interface Agent {

    /** Returns the unique identifier name of the Agent */
    String getName();

    /** Execute agent logic using the shared AgentContext without mutating other agents directly */
    AgentResult execute(AgentContext context);
}
