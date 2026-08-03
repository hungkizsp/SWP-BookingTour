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
public class RiskAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "RiskAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName    = context.getTour() != null ? context.getTour().getTourName()    : "Tour du lịch";
        String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                ? context.getTour().getEndLocation() : "Đà Nẵng";

        String weatherStr = context.getWeather() != null
                ? String.format("Thời tiết tại %s: %s, nhiệt độ %.1f°C. %s",
                    destination,
                    context.getWeather().getForecast(),
                    context.getWeather().getTempCelsius(),
                    context.getWeather().getAdvice())
                : "Thời tiết tại " + destination + " chưa có dữ liệu cụ thể.";

        String systemPrompt = String.format(
                "Bạn là RiskAgent - chuyên gia phân tích rủi ro và lưu ý an toàn cho du khách. " +
                "Phân tích các rủi ro tiềm ẩn và lưu ý quan trọng cho tour '%s' tại %s. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Nêu: rủi ro thời tiết, rủi ro sức khỏe, lưu ý văn hóa địa phương, " +
                "những vật dụng nên mang theo. Viết 3-5 câu ngắn gọn, thực tế.",
                tourName, destination);

        String output = aiGateway.generate(systemPrompt, weatherStr);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể phân tích rủi ro lúc này.")
                .build();
    }
}
