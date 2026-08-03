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
public class MarketingAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "MarketingAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName    = context.getTour() != null ? context.getTour().getTourName()    : "Tour du lịch";
        String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                ? context.getTour().getEndLocation() : "điểm đến";
        String category    = context.getTour() != null && context.getTour().getCategory() != null
                ? context.getTour().getCategory().getCategoryName() : "du lịch";
        String desc        = context.getTour() != null && context.getTour().getDescription() != null
                ? context.getTour().getDescription() : "";

        // If called from admin context, use userPrompt; if from tour context, build tour-specific prompt
        String userInput = (context.getUserPrompt() != null && !context.getUserPrompt().isBlank())
                ? context.getUserPrompt()
                : String.format("Tour '%s' tại %s, loại hình: %s. Mô tả: %s", tourName, destination, category, desc);

        String systemPrompt = String.format(
                "Bạn là MarketingAgent - chuyên gia marketing du lịch. " +
                "Phân tích sức hút và phân khúc khách hàng phù hợp với tour '%s' tại %s. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Nêu: đối tượng khách hàng lý tưởng, điểm bán hàng độc đáo (USP), " +
                "thông điệp marketing gợi cảm hứng ngắn gọn. Viết 3-5 câu.",
                tourName, destination);

        String output = aiGateway.generate(systemPrompt, userInput);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể phân tích marketing lúc này.")
                .build();
    }
}
