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
public class PricingAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "PricingAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName = context.getTour() != null ? context.getTour().getTourName() : "Tour du lịch";
        String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                ? context.getTour().getEndLocation() : "điểm đến";

        String priceInfo = "";
        if (context.getTour() != null) {
            java.math.BigDecimal price = context.getTour().getPrice();
            String bestTime = context.getTour().getBestTime();
            String inclusions = context.getTour().getInclusions();
            priceInfo = String.format("Giá tour: %s VND. Thời điểm tốt nhất: %s. Đã bao gồm: %s",
                    price != null ? price.toPlainString() : "Chưa rõ",
                    bestTime != null ? bestTime : "Quanh năm",
                    inclusions != null ? inclusions : "Xem thêm chi tiết");
        }

        String systemPrompt = String.format(
                "Bạn là PricingAgent - chuyên gia phân tích giá và giá trị tour du lịch. " +
                "Phân tích mức giá tour '%s' đến %s và đưa ra nhận xét về tính cạnh tranh, giá trị nhận được. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Nêu: đánh giá mức giá so với thị trường, các chi phí phát sinh cần lưu ý, " +
                "lời khuyên tiết kiệm thông minh. Viết 3-5 câu ngắn gọn.",
                tourName, destination);

        String output = aiGateway.generate(systemPrompt,
                priceInfo.isEmpty() ? "Phân tích giá trị của tour này" : priceInfo);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể phân tích giá lúc này.")
                .build();
    }
}
