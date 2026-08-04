package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "AILogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "LogID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class AILog extends Base {

    @Column(name = "AgentName", length = 50)
    private String agentName;

    @Column(name = "WorkflowId", length = 100)
    private String workflowId;

    @Builder.Default
    @Column(name = "InputTokens")
    private Integer inputTokens = 0;

    @Builder.Default
    @Column(name = "OutputTokens")
    private Integer outputTokens = 0;

    @Builder.Default
    @Column(name = "LatencyMs")
    private Long latencyMs = 0L;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "ErrorMessage", columnDefinition = "NVARCHAR(500)")
    private String errorMessage;

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
