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
public class RecommendationAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "RecommendationAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName   = context.getTour() != null ? context.getTour().getTourName()    : "Tour du lịch";
        String category   = context.getTour() != null && context.getTour().getCategory() != null
                ? context.getTour().getCategory().getCategoryName() : "Du lịch tổng hợp";
        String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                ? context.getTour().getEndLocation() : "Đà Nẵng";
        String desc        = context.getTour() != null && context.getTour().getDescription() != null
                ? context.getTour().getDescription() : "";

        String systemPrompt = String.format(
                "Bạn là RecommendationAgent - chuyên gia tư vấn trải nghiệm du lịch. " +
                "Đề xuất những trải nghiệm, địa điểm và hoạt động nổi bật nhất cho tour '%s' tại %s, loại hình: %s. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Gợi ý: 3 địa điểm phải ghé thăm, 2 món ẩm thực đặc sản, 1 hoạt động trải nghiệm độc đáo. " +
                "Viết 4-6 câu ngắn gọn, hấp dẫn và cụ thể.",
                tourName, destination, category);

        String output = aiGateway.generate(systemPrompt, "Mô tả tour: " + desc);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể tạo gợi ý lúc này.")
                .build();
    }
}
