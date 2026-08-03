package com.tourbooking.booking.backend.ai.agent;

import com.tourbooking.booking.backend.ai.memory.MemoryService;
import com.tourbooking.booking.backend.ai.orchestrator.Agent;
import com.tourbooking.booking.backend.ai.orchestrator.AgentContext;
import com.tourbooking.booking.backend.ai.orchestrator.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryAgent implements Agent {

    private final MemoryService memoryService;

    @Override
    public String getName() {
        return "MemoryAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        if (context.getUser() != null && context.getBooking() != null) {
            memoryService.updateAfterBooking(context.getUser().getId(), context.getBooking());
            return AgentResult.builder()
                    .agentName(getName())
                    .success(true)
                    .textOutput("Hồ sơ trí nhớ AI của người dùng đã được cập nhật thành công.")
                    .build();
        }
        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput("Chưa có thông tin booking/user để lưu trí nhớ.")
                .build();
    }
}
