package com.tourbooking.booking.backend.ai.agent;

import com.tourbooking.booking.backend.ai.gateway.AIGatewayService;
import com.tourbooking.booking.backend.ai.orchestrator.Agent;
import com.tourbooking.booking.backend.ai.orchestrator.AgentContext;
import com.tourbooking.booking.backend.ai.orchestrator.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportAgent implements Agent {

    private final AIGatewayService aiGateway;

    @Override
    public String getName() {
        return "SupportAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, AgentResult> results = context.getAgentResults();

        StringBuilder aggregatedInput = new StringBuilder("Dưới đây là kết quả phân tích từ các agent chuyên môn:\n\n");
        results.forEach((name, res) -> {
            if (res.isSuccess()) {
                aggregatedInput.append("=== ").append(name).append(" ===\n")
                        .append(res.getTextOutput()).append("\n\n");
            }
        });

        String tourName = context.getTour() != null ? context.getTour().getTourName() : "Tour du lịch";
        String startLoc = context.getTour() != null ? context.getTour().getStartLocation() : "";
        String endLoc = context.getTour() != null ? context.getTour().getEndLocation() : "";

        String systemPrompt = String.format(
                "Bạn là SupportAgent - Trợ lý tổng hợp báo cáo du lịch dành riêng cho tour '%s' (Điểm đi: %s -> Điểm đến: %s).\n" +
                "NHIỆM VỤ: Tổng hợp thông tin từ các agent chuyên môn thành báo cáo AI Travel Report chi tiết, CHÍNH XÁC VÀ BÁM SÁT ĐÚNG TOUR NÀY.\n" +
                "QUY TẮC BẮT BUỘC:\n" +
                "1. Tập trung phân tích và tổng hợp thông tin liên quan đến tour '%s' và các địa điểm thuộc hành trình này (như %s, %s). Tuyệt đối không tự ý thêm các địa danh ngoài lề không liên quan đến tour này.\n" +
                "2. Tuyệt đối KHÔNG sử dụng ký tự Markdown như ###, **, ---, hay dấu gạch ngang.\n" +
                "3. CẤU TRÚC BẮT BUỘC: Chia báo cáo thành ÍT NHẤT 6 phần. Mỗi phần PHẢI có:\n" +
                "   - Dòng đầu tiên: TÊN PHẦN NGẮN GỌN (dưới 60 ký tự, không có dấu chấm cuối)\n" +
                "   - Các dòng tiếp theo: Nội dung chi tiết của phần đó (2-4 câu)\n" +
                "   - Kết thúc phần bằng MỘT DÒNG TRẮNG trước phần tiếp theo\n" +
                "4. Các phần BẮT BUỘC phải có: Tổng quan tour, Trải nghiệm nổi bật, Thời tiết và khí hậu, Đánh giá khách hàng, Lưu ý quan trọng, Lời khuyên cho chuyến đi.\n" +
                "5. Viết sinh động, truyền cảm hứng, sử dụng tiếng Việt tự nhiên.",
                tourName, startLoc, endLoc, tourName, startLoc, endLoc
        );
        String finalReport = aiGateway.generate(systemPrompt, aggregatedInput.toString());

        return AgentResult.builder()
                .agentName(getName())
                .success(true)
                .textOutput(finalReport)
                .build();
    }
}
