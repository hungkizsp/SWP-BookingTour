package com.tourbooking.booking.backend.ai.orchestrator;

import com.tourbooking.booking.backend.ai.tool.WeatherInfo;
import com.tourbooking.booking.backend.model.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    private Tour tour;
    private TourSchedule schedule;
    private User user;
    private Booking booking;
    private List<Review> reviews;
    private WeatherInfo weather;
    private UserAIProfile userMemory;

    private String userPrompt;
    private String workflowId;
    private AIWorkflow workflowType;

    @Builder.Default
    private Map<String, AgentResult> agentResults = new HashMap<>();

    public void addResult(AgentResult result) {
        if (result != null && result.getAgentName() != null) {
            agentResults.put(result.getAgentName(), result);
        }
    }

    public AgentResult getResult(String agentName) {
        return agentResults.get(agentName);
    }
}
