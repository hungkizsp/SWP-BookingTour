package com.tourbooking.booking.backend.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tourbooking.booking.backend.component.GeminiClient;
import com.tourbooking.booking.backend.model.dto.request.TourCompareRequest;
import com.tourbooking.booking.backend.model.dto.response.TourCompareResponse;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.service.TourCompareService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCompareServiceImpl implements TourCompareService {

    private final TourRepository tourRepo;
    private final GeminiClient geminiClient;

    private static class CacheEntry {
        String data;
        Instant expiry;

        CacheEntry(String data, Instant expiry) {
            this.data = data;
            this.expiry = expiry;
        }
    }

    private final Map<String, CacheEntry> compareCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public TourCompareResponse compareTours(TourCompareRequest request) {
        List<Long> requestedIds = request.getTourIds();
        
        // Build cache key: sorted IDs joined by "_"
        List<Long> sortedIds = new ArrayList<>(requestedIds);
        Collections.sort(sortedIds);
        String cacheKey = sortedIds.stream().map(String::valueOf).collect(Collectors.joining("_"));
        
        cleanupCache();

        CacheEntry cached = compareCache.get(cacheKey);
        if (cached != null && cached.expiry.isAfter(Instant.now())) {
            log.info("Returning cached AI comparison for tours: {}", cacheKey);
            return TourCompareResponse.builder().analysis(cached.data).build();
        }

        List<Tour> tours = tourRepo.findAllById(requestedIds);
        
        List<Long> foundIds = tours.stream().map(Tour::getId).collect(Collectors.toList());
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy các tour có ID: " + missingIds);
        }

        String tourContext = buildCompareContext(tours);
        
        String systemPrompt = 
            "Bạn là chuyên viên tư vấn du lịch cấp cao của TourBooking.\n" +
            "Nhiệm vụ của bạn là so sánh các tour được cung cấp dưới đây để giúp khách hàng đưa ra quyết định.\n\n" +
            "NGUYÊN TẮC QUAN TRỌNG:\n" +
            "1. Luôn trả lời bằng tiếng Việt, chuyên nghiệp, khách quan, rõ ràng.\n" +
            "2. CHỈ DỰA VÀO DỮ LIỆU ĐƯỢC CUNG CẤP. Không tự ý bịa thêm thông tin về giá, lịch trình, chính sách, hoặc ưu điểm.\n" +
            "3. Format trả lời bằng Markdown, sử dụng cấu trúc:\n" +
            "   - **Tóm tắt nhanh**: Đánh giá tổng quan sự khác biệt chính giữa các tour.\n" +
            "   - **Phù hợp với ai**: Xác định tập khách hàng tốt nhất cho từng tour.\n" +
            "   - **Điểm cộng & Điểm trừ**: Liệt kê rõ ràng cho từng tour.\n" +
            "   - **Gợi ý của chuyên gia**: Đưa ra lời khuyên cuối cùng nên chọn tour nào trong trường hợp nào.\n\n" +
            "THÔNG TIN CÁC TOUR CẦN SO SÁNH:\n" + 
            tourContext;

        try {
            String analysis = geminiClient.callGeminiCompareRaw(systemPrompt, "Hãy so sánh chi tiết các tour này giúp tôi.");
            
            // Cache the result for 2 hours
            compareCache.put(cacheKey, new CacheEntry(analysis, Instant.now().plus(2, ChronoUnit.HOURS)));
            
            return TourCompareResponse.builder().analysis(analysis).build();
        } catch (Exception e) {
            log.error("Failed to call Gemini for tour comparison", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể phân tích lúc này, vui lòng thử lại sau.");
        }
    }

    private String buildCompareContext(List<Tour> tours) {
        return tours.stream().map(t -> {
            String tourId = String.valueOf(t.getId());
            String name = t.getTourName() != null ? t.getTourName() : "Không tên";
            String price = t.getPrice() != null ? String.format("%,.0f VND", t.getPrice()) : "Liên hệ";
            String duration = t.getDuration() != null ? t.getDuration() + " ngày" : "N/A";
            String from = t.getStartLocation() != null ? t.getStartLocation() : "N/A";
            String to = t.getEndLocation() != null ? t.getEndLocation() : "N/A";
            String rating = t.getRating() != null ? String.format("%.1f/5", t.getRating()) : "Chưa có";
            String transport = t.getTransportType() != null ? t.getTransportType() : "N/A";
            String suitable = t.getSuitableAges() != null ? t.getSuitableAges() : "Mọi lứa tuổi";
            String policy = t.getChildPolicy() != null ? t.getChildPolicy() : "Theo quy định chung";
            String why = t.getWhyChooseUs() != null ? t.getWhyChooseUs() : "Chất lượng đảm bảo";
            
            // Force initialization of lazy relationships
            String category = t.getCategory() != null ? t.getCategory().getCategoryName() : "N/A";
            String city = t.getCity() != null ? t.getCity().getCityName() : "N/A";

            return "--- TOUR " + tourId + " ---\n" +
                   "Tên tour: " + name + "\n" +
                   "Thể loại: " + category + " | Thành phố: " + city + "\n" +
                   "Giá: " + price + "\n" +
                   "Thời gian: " + duration + "\n" +
                   "Hành trình: " + from + " -> " + to + "\n" +
                   "Phương tiện: " + transport + "\n" +
                   "Đánh giá: " + rating + "\n" +
                   "Đối tượng phù hợp: " + suitable + "\n" +
                   "Chính sách trẻ em: " + policy + "\n" +
                   "Điểm nổi bật: " + why + "\n";
        }).collect(Collectors.joining("\n"));
    }

    private void cleanupCache() {
        Instant now = Instant.now();
        compareCache.entrySet().removeIf(entry -> entry.getValue().expiry.isBefore(now));
    }
}
