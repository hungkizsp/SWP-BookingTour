package com.tourbooking.booking.backend.ai.agent;

import com.tourbooking.booking.backend.ai.gateway.AIGatewayService;
import com.tourbooking.booking.backend.ai.orchestrator.Agent;
import com.tourbooking.booking.backend.ai.orchestrator.AgentContext;
import com.tourbooking.booking.backend.ai.orchestrator.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "PlannerAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName  = context.getTour() != null ? context.getTour().getTourName()    : "Tour du lịch";
        String startLoc  = context.getTour() != null ? context.getTour().getStartLocation(): "Đà Nẵng";
        String endLoc    = context.getTour() != null ? context.getTour().getEndLocation()  : "";
        String desc      = context.getTour() != null && context.getTour().getDescription() != null
                ? context.getTour().getDescription() : "Không có mô tả.";
        String itinerary = context.getTour() != null && context.getTour().getItinerary() != null
                ? context.getTour().getItinerary() : "Lịch trình linh hoạt.";

        int duration  = context.getTour() != null ? (context.getTour().getDuration() != null ? context.getTour().getDuration() : 1) : 1;

        String systemPrompt = String.format(
                "Bạn là PlannerAgent - chuyên gia lập kế hoạch hành trình du lịch. " +
                "Phân tích tour '%s' từ %s đến %s, thời lượng %d ngày. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown (không có ###, **, ---). " +
                "Trả về phân tích kế hoạch hành trình theo ngày, điểm nổi bật cần ghé thăm, " +
                "gợi ý thời gian tối ưu cho từng địa điểm. Viết 3-5 câu ngắn gọn, súc tích.",
                tourName, startLoc, endLoc, duration);

        String userInput = String.format(
                "Mô tả tour: %s\nLịch trình chi tiết: %s",
                desc, itinerary);

        String output = aiGateway.generate(systemPrompt, userInput);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể tạo kế hoạch lúc này.")
                .build();
    }
}
