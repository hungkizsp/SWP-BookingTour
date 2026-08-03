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
public class ForecastAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "ForecastAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName    = context.getTour() != null ? context.getTour().getTourName()    : "Tour du lịch";
        String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                ? context.getTour().getEndLocation() : "điểm đến";
        String category    = context.getTour() != null && context.getTour().getCategory() != null
                ? context.getTour().getCategory().getCategoryName() : "du lịch";

        String weatherContext = context.getWeather() != null
                ? String.format("Thời tiết hiện tại: %s, %.1f°C",
                    context.getWeather().getForecast(), context.getWeather().getTempCelsius())
                : "";

        String systemPrompt = String.format(
                "Bạn là ForecastAgent - chuyên gia dự báo xu hướng du lịch. " +
                "Dự báo xu hướng và thời điểm lý tưởng cho tour '%s' loại hình %s tại %s. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Nêu: mùa cao điểm/thấp điểm, thời điểm tốt nhất trong năm, " +
                "xu hướng khách du lịch đến %s. Viết 3-5 câu ngắn gọn, cụ thể.",
                tourName, category, destination, destination);

        String userInput = String.format("Tour: %s, Điểm đến: %s. %s", tourName, destination, weatherContext);

        String output = aiGateway.generate(systemPrompt, userInput);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể dự báo xu hướng lúc này.")
                .build();
    }
}
