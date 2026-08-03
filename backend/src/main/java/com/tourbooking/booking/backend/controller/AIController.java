package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.ai.agent.*;
import com.tourbooking.booking.backend.ai.orchestrator.AIOrchestrator;
import com.tourbooking.booking.backend.ai.orchestrator.AIWorkflow;
import com.tourbooking.booking.backend.ai.orchestrator.AgentContext;
import com.tourbooking.booking.backend.ai.tool.ToolService;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.repository.ReviewRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIOrchestrator aiOrchestrator;
    private final PlannerAgent plannerAgent;
    private final RecommendationAgent recommendationAgent;
    private final ReviewAgent reviewAgent;
    private final RiskAgent riskAgent;
    private final SupportAgent supportAgent;
    private final PricingAgent pricingAgent;
    private final ForecastAgent forecastAgent;
    private final MarketingAgent marketingAgent;
    private final ResourceAgent resourceAgent;

    private final TourRepository tourRepo;
    private final ReviewRepository reviewRepo;
    private final ToolService toolService;

    @PostMapping("/tour/{tourId}/analyze")
    public ApiResponse<Map<String, Object>> analyzeTour(@PathVariable Long tourId) {
        var tour = tourRepo.findById(tourId).orElse(null);
        var reviews = reviewRepo.findByTourId(tourId);
        var weather = toolService.getWeather(tour != null ? tour.getEndLocation() : "Đà Nẵng", null, null);

        AgentContext context = AgentContext.builder()
                .tour(tour)
                .reviews(reviews)
                .weather(weather)
                .build();

        AgentContext resultContext = aiOrchestrator.runWorkflow(
                AIWorkflow.TOUR_ANALYSIS,
                context,
                List.of(
                        plannerAgent,
                        recommendationAgent,
                        reviewAgent,
                        riskAgent,
                        pricingAgent,
                        forecastAgent,
                        marketingAgent,
                        resourceAgent,
                        supportAgent   // always last: aggregates all others
                )
        );

        String finalReport = resultContext.getResult("SupportAgent") != null
                ? resultContext.getResult("SupportAgent").getTextOutput()
                : "Không thể tạo báo cáo phân tích lúc này.";

        log.info("[AIController] Generated finalReport:\n{}", finalReport);

        java.util.Map<String, Object> resultData = new java.util.LinkedHashMap<>();
        resultData.put("report", finalReport);
        resultData.put("planner",        getOutput(resultContext, "PlannerAgent"));
        resultData.put("recommendation", getOutput(resultContext, "RecommendationAgent"));
        resultData.put("reviews",        getOutput(resultContext, "ReviewAgent"));
        resultData.put("risk",           getOutput(resultContext, "RiskAgent"));
        resultData.put("pricing",        getOutput(resultContext, "PricingAgent"));
        resultData.put("forecast",       getOutput(resultContext, "ForecastAgent"));
        resultData.put("marketing",      getOutput(resultContext, "MarketingAgent"));
        resultData.put("resource",       getOutput(resultContext, "ResourceAgent"));

        return ApiResponse.<Map<String, Object>>builder()
                .code(HttpStatus.OK.value())
                .message("Phân tích tour bằng Multi-Agent AI (9 agents) thành công")
                .data(resultData)
                .build();
    }

    @PostMapping("/checkout/analyze")
    public ApiResponse<Map<String, String>> analyzeCheckout(@RequestBody Map<String, Object> request) {
        String userPrompt = "Phân tích đơn hàng: " + request.toString();
        AgentContext context = AgentContext.builder()
                .userPrompt(userPrompt)
                .build();

        AgentContext resultContext = aiOrchestrator.runWorkflow(
                AIWorkflow.CHECKOUT_ADVICE,
                context,
                List.of(pricingAgent, riskAgent, supportAgent)
        );

        String advice = resultContext.getResult("SupportAgent") != null
                ? resultContext.getResult("SupportAgent").getTextOutput()
                : "Giá tour hiện tại rất hợp lý!";

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("Tư vấn đặt tour AI")
                .data(Map.of("advice", advice))
                .build();
    }

    @PostMapping("/admin/forecast")
    public ApiResponse<Map<String, String>> adminForecast(@RequestBody(required = false) Map<String, Object> body) {
        AgentContext context = AgentContext.builder()
                .userPrompt(body != null ? body.toString() : "Dự báo doanh thu và lượng booking tháng tới")
                .build();

        AgentContext resultContext = aiOrchestrator.runWorkflow(
                AIWorkflow.ADMIN_FORECAST,
                context,
                List.of(forecastAgent)
        );

        String output = resultContext.getResult("ForecastAgent") != null
                ? resultContext.getResult("ForecastAgent").getTextOutput() : "";

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("AI Forecast Admin Report")
                .data(Map.of("forecast", output))
                .build();
    }

    @PostMapping("/admin/marketing")
    public ApiResponse<Map<String, String>> adminMarketing(@RequestBody(required = false) Map<String, Object> body) {
        AgentContext context = AgentContext.builder()
                .userPrompt(body != null ? body.toString() : "Đề xuất chiến dịch Marketing mới")
                .build();

        AgentContext resultContext = aiOrchestrator.runWorkflow(
                AIWorkflow.ADMIN_MARKETING,
                context,
                List.of(marketingAgent)
        );

        String output = resultContext.getResult("MarketingAgent") != null
                ? resultContext.getResult("MarketingAgent").getTextOutput() : "";

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("AI Marketing Admin Campaign")
                .data(Map.of("campaign", output))
                .build();
    }

    @PostMapping("/admin/resource")
    public ApiResponse<Map<String, String>> adminResource(@RequestBody(required = false) Map<String, Object> body) {
        AgentContext context = AgentContext.builder()
                .userPrompt(body != null ? body.toString() : "Phân bổ tài nguyên HDV và phương tiện")
                .build();

        AgentContext resultContext = aiOrchestrator.runWorkflow(
                AIWorkflow.ADMIN_RESOURCE,
                context,
                List.of(resourceAgent)
        );

        String output = resultContext.getResult("ResourceAgent") != null
                ? resultContext.getResult("ResourceAgent").getTextOutput() : "";

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("AI Resource Planning Admin Report")
                .data(Map.of("resource", output))
                .build();
    }

    // Helper: safely extract text output from a named agent result
    private String getOutput(com.tourbooking.booking.backend.ai.orchestrator.AgentContext ctx, String agentName) {
        com.tourbooking.booking.backend.ai.orchestrator.AgentResult r = ctx.getResult(agentName);
        return (r != null && r.getTextOutput() != null) ? r.getTextOutput() : "";
    }
}
