package com.tourbooking.booking.backend.ai.logger;

import com.tourbooking.booking.backend.model.entity.AILog;

import java.util.List;

public interface AILogService {

    /** Record single AI execution log */
    void log(String agentName, String workflowId, Long latencyMs, String status, String errorMessage);

    /** Fetch recent AI logs for dashboard */
    List<AILog> getRecentLogs(int limit);
}
