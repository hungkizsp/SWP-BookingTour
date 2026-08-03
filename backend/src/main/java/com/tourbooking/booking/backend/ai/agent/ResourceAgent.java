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
public class ResourceAgent implements Agent {

        private final AIGatewayService aiGateway;

        @Override
        public String getName() {
                return "ResourceAgent";
        }

        @Override
        public AgentResult execute(AgentContext context) {
                String tourName = context.getTour() != null ? context.getTour().getTourName() : "Tour du lịch";
                String destination = context.getTour() != null && context.getTour().getEndLocation() != null
                                ? context.getTour().getEndLocation()
                                : "điểm đến";
                int duration = context.getTour() != null && context.getTour().getDuration() != null
                                ? context.getTour().getDuration()
                                : 1;
                String transport = context.getTour() != null && context.getTour().getTransportType() != null
                                ? context.getTour().getTransportType()
                                : "xe đưa đón";
                String inclusions = context.getTour() != null && context.getTour().getInclusions() != null
                                ? context.getTour().getInclusions()
                                : "";

                // If called from admin context, use userPrompt; if from tour context, build
                // tour-specific prompt
                String userInput = (context.getUserPrompt() != null && !context.getUserPrompt().isBlank())
                                ? context.getUserPrompt()
                                : String.format("Tour '%s' đến %s, %d ngày, phương tiện: %s. Bao gồm: %s",
                                                tourName, destination, duration, transport, inclusions);

                String systemPrompt = String.format(
                                "Bạn là ResourceAgent - chuyên gia vận hành và hậu cần du lịch. " +
                                                "Đánh giá yêu cầu vận hành cho tour '%s' tại %s (%d ngày, phương tiện: %s). "
                                                +
                                                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                                                "Nêu: yêu cầu hướng dẫn viên, phương tiện di chuyển phù hợp, " +
                                                "lưu ý về sức chứa và tổ chức đoàn. Viết 3-5 câu ngắn gọn.",
                                tourName, destination, duration, transport);

                String output = aiGateway.generate(systemPrompt, userInput);

                return AgentResult.builder()
                                .agentName(getName())
                                .success(true)
                                .textOutput(output != null ? output : "Không thể phân tích tài nguyên lúc này.")
                                .build();
        }
}
