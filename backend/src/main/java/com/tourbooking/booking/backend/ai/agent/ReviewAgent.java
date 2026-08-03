package com.tourbooking.booking.backend.ai.agent;

import com.tourbooking.booking.backend.ai.gateway.AIGatewayService;
import com.tourbooking.booking.backend.ai.orchestrator.Agent;
import com.tourbooking.booking.backend.ai.orchestrator.AgentContext;
import com.tourbooking.booking.backend.ai.orchestrator.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "ReviewAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String tourName = context.getTour() != null ? context.getTour().getTourName() : "Tour du lịch";

        String reviewsRaw = (context.getReviews() != null && !context.getReviews().isEmpty())
                ? context.getReviews().stream().limit(8)
                    .map(r -> String.format("%d/5 sao: %s", r.getRating(), r.getComment()))
                    .collect(Collectors.joining("\n"))
                : "Chưa có đánh giá nào từ khách hàng.";

        double avgRating = (context.getReviews() != null && !context.getReviews().isEmpty())
                ? context.getReviews().stream()
                    .mapToInt(r -> r.getRating() != null ? r.getRating() : 0)
                    .average().orElse(0)
                : 0;

        String systemPrompt = String.format(
                "Bạn là ReviewAgent - chuyên gia phân tích đánh giá và phản hồi du khách. " +
                "Phân tích các đánh giá của tour '%s'. " +
                "VIẾT BẰNG TIẾNG VIỆT. Không dùng Markdown. " +
                "Tổng hợp: điểm mạnh được khách yêu thích, điểm cần cải thiện, " +
                "xu hướng cảm xúc tổng thể. Viết 3-5 câu ngắn gọn.",
                tourName);

        String userInput = String.format(
                "Điểm đánh giá trung bình: %.1f/5\nCác đánh giá:\n%s",
                avgRating, reviewsRaw);

        String output = aiGateway.generate(systemPrompt, userInput);

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(output != null ? output : "Không thể phân tích đánh giá lúc này.")
                .build();
    }
}
