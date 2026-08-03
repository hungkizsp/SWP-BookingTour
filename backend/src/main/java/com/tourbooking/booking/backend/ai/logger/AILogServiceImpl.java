package com.tourbooking.booking.backend.ai.logger;

import com.tourbooking.booking.backend.model.entity.AILog;
import com.tourbooking.booking.backend.repository.AILogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AILogServiceImpl implements AILogService {

    private final AILogRepository aiLogRepo;

    @Override
    public void log(String agentName, String workflowId, Long latencyMs, String status, String errorMessage) {
        try {
            AILog entry = new AILog();
            entry.setAgentName(agentName);
            entry.setWorkflowId(workflowId);
            entry.setLatencyMs(latencyMs != null ? latencyMs : 0L);
            entry.setStatus(status != null ? status : "SUCCESS");
            String safeError = errorMessage;
            if (safeError != null && safeError.length() > 450) {
                safeError = safeError.substring(0, 450);
            }
            entry.setErrorMessage(safeError);

            aiLogRepo.save(entry);
        } catch (Exception e) {
            log.error("[AILogService] Failed to save AI log: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AILog> getRecentLogs(int limit) {
        return aiLogRepo.findTop50ByOrderByIdDesc();
    }
}
