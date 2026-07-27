package com.tourbooking.booking.backend.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixDatabaseComponent implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        log.info("--- STARTING DATABASE INITIALIZATION ---");

        // Tự động kiểm tra và cập nhật Database Schema nếu thiếu cột
        migrateSchemaIfNeeded();

        try {
            // 1. Seed base lookup data (categories, cities)
            seedCategories();
            seedCities();

            // 2. Seed Admin User
            seedAdminUser();

            // 3. Seed sample Tours (only if empty)
            seedSampleTours();

            // 3b. Seed tour images (delete old and re-seed to update URLs to local paths)
            try {
                jdbcTemplate.execute("DELETE FROM TourImages");
            } catch (Exception e) {
                log.warn("Could not delete old tour images: {}", e.getMessage());
            }
            seedTourImages();

            // Seed tour itinerary days for meals and accommodations
            seedTourItineraryDays();

            // 4. Seed Premium Content for known tours
            seedPremiumTourData();

            // Seed WhyChooseUs for all tours with 150+ words
            seedWhyChooseUsForAllTours();

            // 5. Ensure ALL tours have detailed itineraries
            ensureDetailedItinerariesForAllTours();

            // 6. Ensure upcoming schedules exist for all tours
            ensureUpcomingSchedulesForAllTours();

            // 7. Ensure highlights exist for all tours
            ensureHighlightsForAllTours();

            // 8. Seed FAQs (global + per-tour)
            seedFaqs();

            // 9. Ensure extra tours for paging test
            seedExtraToursForPaging();

        } catch (Exception e) {
            log.error("Initialization error (continuing app startup): {}", e.getMessage());
        }

        log.info("--- DATABASE INITIALIZATION COMPLETED ---");
    }

    private void migrateSchemaIfNeeded() {
        try {
            log.info("Checking and migrating database schema...");

            // 1. Check and add AssignedStaffID to ChatSessions
            try {
                jdbcTemplate.execute(
                        "IF COL_LENGTH('dbo.ChatSessions', 'AssignedStaffID') IS NULL " +
                                "BEGIN " +
                                "    ALTER TABLE dbo.ChatSessions ADD AssignedStaffID BIGINT NULL; " +
                                "    ALTER TABLE dbo.ChatSessions ADD CONSTRAINT FK_ChatSessions_Staff FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Users(UserID); "
                                +
                                "END");
                log.info("Schema migration: AssignedStaffID column checked/added to ChatSessions.");
            } catch (Exception e) {
                log.warn("Failed to check/add AssignedStaffID to ChatSessions: {}", e.getMessage());
            }

            // 2. Check and add OriginalBookingStatus to RefundRequests
            try {
                jdbcTemplate.execute(
                        "IF COL_LENGTH('dbo.RefundRequests', 'OriginalBookingStatus') IS NULL " +
                                "BEGIN " +
                                "    ALTER TABLE dbo.RefundRequests ADD OriginalBookingStatus NVARCHAR(50) NULL; " +
                                "END");
                log.info("Schema migration: OriginalBookingStatus column checked/added to RefundRequests.");
            } catch (Exception e) {
                log.warn("Failed to check/add OriginalBookingStatus to RefundRequests: {}", e.getMessage());
            }

            // 3. Check and add EmailVerified to Users
            try {
                jdbcTemplate.execute(
                        "IF COL_LENGTH('dbo.Users', 'EmailVerified') IS NULL " +
                                "BEGIN " +
                                "    ALTER TABLE dbo.Users ADD EmailVerified BIT NOT NULL CONSTRAINT DF_Users_EmailVerified DEFAULT 0; " +
                                "END");
                jdbcTemplate.execute(
                        "IF COL_LENGTH('dbo.Users', 'EmailVerified') IS NOT NULL " +
                                "BEGIN " +
                                "    UPDATE dbo.Users SET EmailVerified = 1; " +
                                "END");
                log.info("Schema migration: EmailVerified column checked/added to Users.");
            } catch (Exception e) {
                log.warn("Failed to check/add EmailVerified to Users: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Schema migration error: {}", e.getMessage());
        }
    }

    // =====================================================================
    // BASE DATA: CATEGORIES
    // =====================================================================
    private void seedCategories() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Categories", Integer.class);
            if (count != null && count > 0)
                return;

            String[][] cats = {
                    { "Du lịch biển đảo", "Các tour tham quan, nghỉ dưỡng tại bãi biển và đảo" },
                    { "Du lịch văn hóa", "Khám phá di sản, lịch sử và văn hóa bản địa" },
                    { "Du lịch sinh thái", "Trekking, khám phá thiên nhiên và rừng núi" },
                    { "Du lịch mạo hiểm", "Các hoạt động thể thao mạo hiểm ngoài trời" },
                    { "Du lịch ẩm thực", "Trải nghiệm ẩm thực đặc sắc các vùng miền" },
                    { "Du lịch nghỉ dưỡng", "Resort, spa và các gói nghỉ dưỡng cao cấp" },
                    { "Du lịch tâm linh", "Thăm chùa chiền, đền đài và các địa điểm tâm linh" },
                    { "Du lịch thành phố", "City tour và khám phá cuộc sống đô thị" }
            };

            for (String[] c : cats) {
                jdbcTemplate.update(
                        "INSERT INTO Categories (CategoryName, Description, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        c[0], c[1]);
            }
            log.info("Seed: Inserted {} categories", cats.length);
        } catch (Exception e) {
            log.error("Category seeding error: {}", e.getMessage());
        }
    }

    // =====================================================================
    // BASE DATA: CITIES
    // =====================================================================
    private void seedCities() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Cities", Integer.class);
            if (count != null && count > 0)
                return;

            Object[][] cities = {
                    { "Đà Nẵng", 16.054407, 108.202167 },
                    { "Hội An", 15.879799, 108.335106 },
                    { "Hà Nội", 21.027764, 105.834160 },
                    { "Hồ Chí Minh", 10.823099, 106.629664 },
                    { "Nha Trang", 12.238791, 109.196749 },
                    { "Đà Lạt", 11.940419, 108.458313 },
                    { "Huế", 16.467397, 107.590866 },
                    { "Phú Quốc", 10.289360, 103.984100 },
                    { "Hạ Long", 20.951916, 107.074580 },
                    { "Sapa", 22.336523, 103.843857 }
            };

            for (Object[] c : cities) {
                jdbcTemplate.update(
                        "INSERT INTO Cities (CityName, CenterLatitude, CenterLongitude, CreatedAt, UpdatedAt) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        c[0], c[1], c[2]);
            }
            log.info("Seed: Inserted {} cities", cities.length);
        } catch (Exception e) {
            log.error("City seeding error: {}", e.getMessage());
        }
    }

    // =====================================================================
    // SAMPLE TOURS (only runs when Tours table is empty)
    // =====================================================================
    private void seedSampleTours() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Tours", Integer.class);
            if (count != null && count > 0)
                return;

            // Fetch category/city IDs
            Long catBien = getCategoryId("Du lịch biển đảo");
            Long catVanHoa = getCategoryId("Du lịch văn hóa");
            Long catSinhThai = getCategoryId("Du lịch sinh thái");
            Long catNghiDuong = getCategoryId("Du lịch nghỉ dưỡng");
            Long catAmThuc = getCategoryId("Du lịch ẩm thực");

            Long cityDaNang = getCityId("Đà Nẵng");
            Long cityHoiAn = getCityId("Hội An");
            Long cityHaNoi = getCityId("Hà Nội");
            Long cityNhaTrang = getCityId("Nha Trang");
            Long cityDaLat = getCityId("Đà Lạt");
            Long cityPhuQuoc = getCityId("Phú Quốc");
            Long cityHaLong = getCityId("Hạ Long");
            Long citySapa = getCityId("Sapa");
            Long cityHue = getCityId("Huế");

            Object[][] tours = {
                    // {TourName, Price, Duration, StartLocation, TransportType, CategoryID, CityID,
                    // Latitude, Longitude, BestTime, SuitableAges}
                    { "Tour Bà Nà Hills - Cầu Vàng", 850000, 1, "Đà Nẵng", "BUS", catVanHoa, cityDaNang, 16.0229,
                            107.9889, "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Hội An Phố Cổ Về Đêm", 650000, 1, "Đà Nẵng", "BUS", catVanHoa, cityHoiAn, 15.8801, 108.3380,
                            "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Cù Lao Chàm Lặn Ngắm San Hô", 750000, 1, "Hội An", "BOAT", catBien, cityHoiAn, 15.9938,
                            108.5233, "Tháng 3 - Tháng 8", "Từ 5 tuổi trở lên" },
                    { "Tour Ngũ Hành Sơn - Hội An", 550000, 1, "Đà Nẵng", "BUS", catVanHoa, cityDaNang, 16.0021,
                            108.2621, "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Bán Đảo Sơn Trà - Linh Ứng Tự", 450000, 1, "Đà Nẵng", "BUS", catSinhThai, cityDaNang,
                            16.1129, 108.2811, "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Phú Quốc 3N2Đ", 2500000, 3, "Hồ Chí Minh", "PLANE", catNghiDuong, cityPhuQuoc, 10.2897,
                            103.9840, "Tháng 11 - Tháng 4", "Mọi lứa tuổi" },
                    { "Tour Hạ Long Bay 2N1Đ", 1800000, 2, "Hà Nội", "BUS", catBien, cityHaLong, 20.9519, 107.0745,
                            "Tháng 3 - Tháng 11", "Mọi lứa tuổi" },
                    { "Tour Sa Pa Trekking 3N2Đ", 1500000, 3, "Hà Nội", "TRAIN", catSinhThai, citySapa, 22.3365,
                            103.8438, "Tháng 9 - Tháng 11", "Từ 12 tuổi trở lên" },
                    { "Tour Đà Lạt Mộng Mơ 3N2Đ", 1200000, 3, "Hồ Chí Minh", "BUS", catNghiDuong, cityDaLat, 11.9404,
                            108.4583, "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Nha Trang Biển Xanh 3N2Đ", 1600000, 3, "Hà Nội", "PLANE", catBien, cityNhaTrang, 12.2388,
                            109.1967, "Tháng 1 - Tháng 8", "Mọi lứa tuổi" },
                    { "Tour Huế Cố Đô 2N1Đ", 900000, 2, "Đà Nẵng", "BUS", catVanHoa, cityHue, 16.4674, 107.5909,
                            "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Hà Nội - Hạ Long 4N3Đ", 3200000, 4, "Hà Nội", "BUS", catBien, cityHaLong, 20.9519, 107.0745,
                            "Tháng 3 - Tháng 10", "Mọi lứa tuổi" },
            };

            String insertSql = "INSERT INTO Tours (TourName, Price, Duration, StartLocation, TransportType, CategoryID, CityID, Latitude, Longitude, BestTime, SuitableAges, Rating, Source, CreatedAt, UpdatedAt) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 4.5, 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            for (Object[] t : tours) {
                jdbcTemplate.update(insertSql, t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10]);
            }
            log.info("Seed: Inserted {} sample tours", tours.length);
        } catch (Exception e) {
            log.error("Sample tour seeding error: {}", e.getMessage());
        }
    }

    // =====================================================================
    // EXTRA TOURS for paging test (inserts only if missing by name)
    // =====================================================================
    private void seedExtraToursForPaging() {
        try {
            Long catBien = getCategoryId("Du lịch biển đảo");
            Long catSinhThai = getCategoryId("Du lịch sinh thái");
            Long cityDaNang = getCityId("Đà Nẵng");
            Long cityHoiAn = getCityId("Hội An");

            Object[][] extraTours = {
                    { "Tour Phố Đêm Mỹ Khê - Hưởng Vị Biển Xanh",
                            35000, 1, "Đà Nẵng", "Bộ / Xe điện", catBien, cityDaNang, 16.0621, 108.2503,
                            "Quanh năm", "Mọi lứa tuổi" },
                    { "Tour Rừng Dừa Bảy Mẫu - Chao Thung Đảo",
                            40000, 1, "Hội An", "Thúng / Xe điện", catSinhThai, cityHoiAn, 15.8750, 108.3512,
                            "Quanh năm", "Gia đình có trẻ em" },
            };

            String insertSql = "INSERT INTO Tours (TourName, Price, Duration, StartLocation, TransportType, CategoryID, CityID, Latitude, Longitude, BestTime, SuitableAges, Rating, Source, CreatedAt, UpdatedAt) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 4.7, 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            int inserted = 0;
            for (Object[] t : extraTours) {
                String tourName = (String) t[0];
                Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Tours WHERE TourName = ?",
                        Integer.class, tourName);
                if (exists != null && exists > 0)
                    continue;
                jdbcTemplate.update(insertSql, t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10]);
                inserted++;
            }
            if (inserted > 0)
                log.info("Seed: Inserted {} extra tours for paging test", inserted);
        } catch (Exception e) {
            log.error("Extra tour seeding error: {}", e.getMessage());
        }
    }

    private Long getCategoryId(String name) {
        try {
            return jdbcTemplate.queryForObject("SELECT CategoryID FROM Categories WHERE CategoryName = ?", Long.class,
                    name);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getCityId(String name) {
        try {
            return jdbcTemplate.queryForObject("SELECT CityID FROM Cities WHERE CityName = ?", Long.class, name);
        } catch (Exception e) {
            return null;
        }
    }

    // =====================================================================
    // TOUR IMAGES (seed real photos from Unsplash for each tour)
    // =====================================================================
    private void seedTourImages() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TourImages", Integer.class);
            if (count != null && count > 0)
                return;

            // Map: keyword in TourName -> list of image URLs
            Object[][] imageMap = {
                    { "Bà Nà", new String[] {
                            "/uploads/TourImg/banahill-1.jpg",
                            "/uploads/TourImg/banahill-2.jpg",
                            "/uploads/TourImg/banahill-3.jpg"
                    } },
                    { "Hội An", new String[] {
                            "/uploads/TourImg/hoian-1.jpg",
                            "/uploads/TourImg/hoian-2.jpg",
                            "/uploads/TourImg/hoian-3.jpg"
                    } },
                    { "Cù Lao Chàm", new String[] {
                            "/uploads/TourImg/culaocham-1.jpg",
                            "/uploads/TourImg/cu-lao-cham-2.jpg",
                            "/uploads/TourImg/cu-lao-cham-dao-ngoc-bich-key-3.webp"
                    } },
                    { "Ngũ Hành Sơn", new String[] {
                            "/uploads/TourImg/chua-linh-ung-1.webp",
                            "/uploads/TourImg/chua-linh-ung-2.webp",
                            "/uploads/TourImg/chua-linh-ung-3.webp"
                    } },
                    { "Sơn Trà", new String[] {
                            "/uploads/TourImg/chua-linh-ung-1.webp",
                            "/uploads/TourImg/chua-linh-ung-2.webp",
                            "/uploads/TourImg/chua-linh-ung-3.webp"
                    } },
                    { "Phú Quốc", new String[] {
                            "/uploads/TourImg/cu-lao-cham-2.jpg"
                    } },
                    { "Hạ Long", new String[] {
                            "/uploads/TourImg/culaocham-1.jpg"
                    } },
                    { "Sa Pa", new String[] {
                            "/uploads/TourImg/banahill-1.jpg"
                    } },
                    { "Đà Lạt", new String[] {
                            "/uploads/TourImg/hoian-1.jpg"
                    } },
                    { "Nha Trang", new String[] {
                            "/uploads/TourImg/culaocham-1.jpg"
                    } },
                    { "Huế", new String[] {
                            "/uploads/TourImg/hue-1.jpg",
                            "/uploads/TourImg/hue-2.jpg",
                            "/uploads/TourImg/hue-3.jpg"
                    } },
                    { "Hà Nội", new String[] {
                            "/uploads/TourImg/banahill-1.jpg"
                    } },
                    { "Mỹ Sơn", new String[] {
                            "/uploads/TourImg/champa.jpg",
                            "/uploads/TourImg/champa-2.jpg",
                            "/uploads/TourImg/champa-3.jpg"
                    } },
                    { "Food", new String[] {
                            "/uploads/TourImg/food-1.jpg",
                            "/uploads/TourImg/food-2.jpg",
                            "/uploads/TourImg/food-3.webp"
                    } },
                    { "Ẩm thực", new String[] {
                            "/uploads/TourImg/food-1.jpg",
                            "/uploads/TourImg/food-2.jpg",
                            "/uploads/TourImg/food-3.webp"
                    } },
                    { "Dừa Bảy Mẫu", new String[] {
                            "/uploads/TourImg/dua-bay-mau-1.jpg",
                            "/uploads/TourImg/dua-bay-mau-2.jpg",
                            "/uploads/TourImg/dua-bay-mau-3.jpg"
                    } },
                    { "Mỹ Khê", new String[] {
                            "/uploads/TourImg/Tour Phố Đêm Mỹ Khê - Hưởng Vị Biển Xanh-1.jpg",
                            "/uploads/TourImg/Tour Phố Đêm Mỹ Khê - Hưởng Vị Biển Xanh-2.jpg",
                            "/uploads/TourImg/Tour Phố Đêm Mỹ Khê - Hưởng Vị Biển Xanh-3.jpg"
                    } },
                    { "Dừa Bảy Mẫu", new String[] {
                            "/uploads/TourImg/Tour Rừng Dừa Bảy Mẫu - Chao Thúng Đảo-1.jpg",
                            "/uploads/TourImg/Tour Rừng Dừa Bảy Mẫu - Chao Thúng Đảo-2.jpg",
                            "/uploads/TourImg/Tour Rừng Dừa Bảy Mẫu - Chao Thúng Đảo-3.jpg"
                    } }
            };

            int total = 0;
            for (Object[] entry : imageMap) {
                String keyword = (String) entry[0];
                String[] urls = (String[]) entry[1];

                List<Map<String, Object>> tours = jdbcTemplate.queryForList(
                        "SELECT TourID FROM Tours WHERE TourName LIKE ?", "%" + keyword + "%");

                for (Map<String, Object> t : tours) {
                    Long tourId = ((Number) t.get("TourID")).longValue();
                    for (String url : urls) {
                        jdbcTemplate.update(
                                "INSERT INTO TourImages (TourID, ImageURL, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                tourId, url);
                        total++;
                    }
                }
            }
            log.info("Seed: Inserted {} tour images", total);
        } catch (Exception e) {
            log.error("Tour image seeding error: {}", e.getMessage());
        }
    }

    private void seedFaqs() {
        try {
            // Seed Global FAQs
            Integer globalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TourFaqs WHERE TourID IS NULL",
                    Integer.class);
            if (globalCount != null && globalCount == 0) {
                String[][] globalFaqs = {
                        { "Chính sách hoàn/hủy tour như thế nào?",
                                "Nếu hủy trước 7 ngày khởi hành, quý khách được hoàn 100%. Nếu hủy trước 3-6 ngày, hoàn 50%. Trong vòng 48 giờ trước chuyến đi không được hoàn tiền. Quy định áp dụng trừ các trường hợp bất khả kháng như thời tiết xấu hoặc dịch bệnh." },
                        { "Trẻ em có được miễn phí hoặc giảm giá không?",
                                "Trẻ em cao dưới 1 mét được miễn phí 100%. Trẻ em từ 1m đến 1.4m tính 70% giá tour người lớn. Từ 1.4m trở lên tính như người lớn." },
                        { "Giá tour đã bao gồm phí tham quan chưa?",
                                "Giá tour đa số đều bao gồm xe đưa đón, hướng dẫn viên, vé tham quan các điểm trong chương trình và bữa ăn (tùy tour). Vui lòng xem kỹ mục Bao gồm/Không bao gồm ở chuyến đi cụ thể." },
                        { "Làm sao để tôi nhận được xác nhận đặt tour?",
                                "Khi quý khách đặt tour và thanh toán thành công (hoặc chọn thanh toán sau), hệ thống sẽ hiển thị mã giao dịch và trạng thái đơn tại mục 'Tour đã đặt'. Quý khách cũng có thể chụp màn hình giao dịch lúc thanh toán xong." }
                };
                for (String[] faq : globalFaqs) {
                    jdbcTemplate.update(
                            "INSERT INTO TourFaqs (Question, Answer, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            faq[0], faq[1]);
                }
                log.info("Seed: Inserted Global FAQs");
            }

            // Seed Tour-specific FAQs for all tours (if they don't have any)
            List<Map<String, Object>> toursToSeed = jdbcTemplate.queryForList(
                    "SELECT TourID, TourName FROM Tours t WHERE NOT EXISTS (SELECT 1 FROM TourFaqs f WHERE f.TourID = t.TourID)");

            for (Map<String, Object> t : toursToSeed) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String tourName = t.get("TourName") == null ? "này" : String.valueOf(t.get("TourName"));

                String[][] customFaqs = {
                        { "Tour " + tourName + " có phù hợp với người cao tuổi không?",
                                "Lịch trình được thiết kế khá nhẹ nhàng và có nhiều điểm dừng chân nghỉ ngơi. Tuy nhiên, xin lưu ý quý khách cao tuổi nên mang theo thuốc cá nhân và báo trước với HDV để được chăm sóc tốt nhất." },
                        { "Nên mang theo gì khi tham gia trải nghiệm này?",
                                "Quý khách nên chuẩn bị giày đi bộ thoải mái, áo khoác mỏng, mũ nón, kem chống nắng và điện thoại/máy ảnh đầy pin để ghi lại những khoảnh khắc đẹp." }
                };
                for (String[] faq : customFaqs) {
                    jdbcTemplate.update(
                            "INSERT INTO TourFaqs (TourID, Question, Answer, CreatedAt, UpdatedAt) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            tourId, faq[0], faq[1]);
                }
            }
            if (!toursToSeed.isEmpty()) {
                log.info("Seed: Inserted specific FAQs for {} tours", toursToSeed.size());
            }
        } catch (Exception e) {
            log.error("FAQ Seeding error: {}", e.getMessage());
        }
    }

    private void seedPremiumTourData() {
        try {
            // Detailed Itinerary JSON Strings
            String banaItinerary = "[" +
                    "{\"title\":\"08:00 - Khởi hành đón khách\",\"content\":\"Bắt đầu hành trình khám phá Bà Nà Hills đầy thú vị. Đội ngũ hướng dẫn viên chuyên nghiệp và tài xế nhiệt tình của chúng tôi sẽ đón quý khách tại điểm hẹn trung tâm Đà Nẵng hoặc tại sảnh khách sạn bằng xe du lịch đời mới, tiện nghi. Quý khách sẽ được giới thiệu về lịch trình chi tiết và những lưu ý cần thiết để có một chuyến đi trọn vẹn và thoải mái nhất.\"},"
                    +
                    "{\"title\":\"09:30 - Check-in Cầu Vàng\",\"content\":\"Quý khách di chuyển bằng hệ thống cáp treo đạt nhiều kỷ lục thế giới để đến với biểu tượng Cầu Vàng (Golden Bridge). Tại đây, bạn sẽ được tự do chụp hình check-in với đôi bàn tay khổng lồ nâng đỡ dải lụa vàng giữa mây ngàn hung vĩ. Hướng dẫn viên sẽ hỗ trợ quý khách có được những góc ảnh đẹp nhất và kể về ý tưởng kiến trúc độc đáo của công trình mang tầm quốc tế này.\"},"
                    +
                    "{\"title\":\"12:00 - Ăn trưa Buffet\",\"content\":\"Thưởng thức bữa trưa buffet đẳng cấp tại nhà hàng sang trọng trên đỉnh Bà Nà. Thực đơn vô cùng phong phú với hơn 100 món ăn từ ẩm thực Việt Nam truyền thống đến tinh hoa ẩm thực Á - Âu hiện đại. Không gian nhà hàng rộng rãi, thoáng mát cùng sự phục vụ tận tâm sẽ giúp quý khách nạp lại năng lượng tuyệt vời sau buổi sáng tham quan sôi động.\"},"
                    +
                    "{\"title\":\"13:30 - Fantasy Park & Làng Pháp\",\"content\":\"Tự do khám phá khu vui chơi trong nhà Fantasy Park lớn nhất Việt Nam với nhiều trò chơi hấp dẫn cho mọi lứa tuổi. Sau đó, quý khách dạo bước quanh Làng Pháp (French Village) - nơi tái hiện một châu Âu thu nhỏ đầy lãng mạn với những lâu đài cổ kính, quảng trường thơ mộng và những con phố lát đá nghệ thuật, mang lại cảm giác như đang lạc giữa lòng nước Pháp cổ xưa.\"},"
                    +
                    "{\"title\":\"16:00 - Tạm biệt Bà Nà\",\"content\":\"Tập trung tại ga cáp treo để bắt đầu hành trình xuống núi. Một lần nữa quý khách được ngắm nhìn toàn cảnh rừng nguyên sinh Sơn Trà và vịnh Đà Nẵng từ trên cao trong ánh hoàng hôn dịu dàng. Xe và hướng dẫn viên sẽ đưa quý khách trở về điểm đón ban đầu, kết thúc tốt đẹp hành trình khám phá chốn bồng lai tiên cảnh và hẹn gặp lại trong những chuyến đi tiếp theo.\"}"
                    +
                    "]";

            String hanoiItinerary = "[" +
                    "{\"title\":\"15:30 - Khởi hành đi Hội An\",\"content\":\"Xe và hướng dẫn viên đón đoàn tại điểm hẹn, bắt đầu hành trình tham quan Phố cổ Hội An - Di sản văn hóa thế giới. Trên đường đi, quý khách sẽ được nghe kể về lịch sử giao thương sầm uất của thương cảng Hội An xưa, những nét văn hóa giao thoa độc đáo giữa Việt Nam, Nhật Bản và Trung Hoa vẫn còn lưu giữ vẹn nguyên cho đến ngày nay.\"},"
                    +
                    "{\"title\":\"17:00 - Tham quan Phố Cổ\",\"content\":\"Dạo bước trên những con phố nhỏ rêu phong, chiêm bái Chùa Cầu biểu tượng có tuổi đời hơn 400 năm. Tiếp tục tham quan Hội quán Phước Kiến, Nhà cổ Tân Ký với kiến trúc chạm trổ tinh xảo. Quý khách sẽ được hòa mình vào không gian yên bình, ngắm nhìn những chiếc đèn lồng thủ công rực rỡ sắc màu được treo khắp các hiên nhà, tạo nên một vẻ đẹp lung linh và đầy hoài niệm.\"},"
                    +
                    "{\"title\":\"18:30 - Ẩm thực Hội An\",\"content\":\"Thưởng thức bữa tối nồng ấm với các món ăn đặc sản nổi tiếng như Cao lầu, Mì Quảng, Bánh bao - Bánh vạc. Mỗi món ăn đều mang hương vị đặc trưng riêng biệt của vùng đất Quảng Nam, được chế biến từ những nguyên liệu tươi ngon nhất. Không gian nhà hàng ấm cúng bên dòng sông Hoài thơ mộng sẽ mang lại cảm giác thư thái và ngon miệng cho mỗi thực khách.\"},"
                    +
                    "{\"title\":\"20:00 - Thả hoa đăng sông Hoài\",\"content\":\"Trải nghiệm đi thuyền trên dòng sông Hoài thơ mộng và tự tay thả những chiếc đèn hoa đăng lung linh. Quý khách có thể gửi gắm những tâm nguyện, ước mong bình an và may mắn theo dòng nước. Ánh sáng của hàng ngàn chiếc đèn lồng phản chiếu xuống mặt nước hòa cùng âm thanh hò khoan xứ Quảng sẽ tạo nên những kỷ niệm tuyệt đẹp và sâu lắng trong hành trình khám phá đêm Hội An.\"},"
                    +
                    "{\"title\":\"21:00 - Trở về Đà Nẵng\",\"content\":\"Tập trung lên xe và bắt đầu hành trình trở về lại thành phố Đà Nẵng năng động. Quý khách có thời gian nghỉ ngơi và chia sẻ những ấn tượng đẹp về Phố Hội với bạn bè sau một chuyến đi ý nghĩa. Hướng dẫn viên chân thành cảm ơn và chào tạm biệt đoàn tại điểm đón ban đầu, khép lại chương trình tour Hội An đầy màu sắc văn hóa và cảm xúc khó quên.\"}"
                    +
                    "]";

            String chamItinerary = "[" +
                    "{\"title\":\"08:00 - Đón khách & Ra cảng\",\"content\":\"Xe đón quý khách tại khách sạn và di chuyển đến cảng Cửa Đại. Tại đây, quý khách sẽ được hướng dẫn làm thủ tục lên tàu cao tốc đời mới để bắt đầu hành trình vượt sóng ra đảo Cù Lao Chàm. Cảm giác lướt đi trên mặt biển bao la và làn gió mát lành sẽ khiến mọi mệt mỏi tan biến, mang lại sự phấn khích cho một ngày khám phá biển đảo hoang sơ phía trước.\"},"
                    +
                    "{\"title\":\"09:30 - Lặn ngắm san hô\",\"content\":\"Tàu đưa đoàn đến Bãi Xếp hoặc Bãi Ông để thực hiện hoạt động lặn ngắm san hô (snorkeling). Với làn nước trong xanh nhìn tận đáy, quý khách sẽ được chiêm ngưỡng thế giới đại dương rực rỡ với hàng trăm loài san hô đa dạng và những đàn cá nhỏ đầy màu sắc bơi lội xung quanh. Đội ngũ cứu hộ chuyên nghiệp sẽ luôn túc trực để đảm bảo an toàn tuyệt đối cho mọi thành viên.\"},"
                    +
                    "{\"title\":\"12:00 - Buffet hải sản tươi\",\"content\":\"Thưởng thức bữa trưa hải sản tươi sống được chế biến theo phong cách dân dã của ngư dân đảo tại nhà hàng Bãi Ông. Các món ăn đặc sản như ốc vú nàng, cua đá, rau rừng Cù Lao ăn kèm mắm nêm sẽ mang lại hương vị khó quên. Không gian nhà hàng mở hướng biển giúp quý khách vừa dùng bữa vừa tận hưởng tiếng sóng vỗ rì rào và làn gió biển thổi vào mát rượi.\"},"
                    +
                    "{\"title\":\"14:00 - Tham quan di tích đảo\",\"content\":\"Hướng dẫn viên đưa quý khách tham quan các di tích lịch sử quan trọng trên đảo như Chùa Hải Tạng cổ kính - nơi cầu may của ngư dân, Giếng cổ người Chăm với nguồn nước ngọt không bao giờ cạn. Quý khách còn được ghé thăm khu bảo tồn biển để hiểu về các loài sinh vật quý hiếm và ý thức bảo vệ môi trường biển vô giá của cụm đảo được UNESCO công nhận là khu dự trữ sinh quyển thế giới.\"},"
                    +
                    "{\"title\":\"15:30 - Về lại đất liền\",\"content\":\"Chào tạm biệt Cù Lao Chàm, tàu cao tốc đưa đoàn trở về cảng Cửa Đại. Xe sẽ chờ sẵn để đón quý khách trở về lại các điểm ban đầu tại Đà Nẵng hoặc Hội An. Kết thúc một ngày hành trình đầy ý nghĩa với muôn vàn trải nghiệm tuyệt vời giữa đại dương xanh thẳm. Chúng tôi hy vọng chuyến đi đã mang lại cho bạn những phút giây thư giãn và những bức ảnh kỷ niệm thật đẹp bên người thân.\"}"
                    +
                    "]";

            String marbleHoiAnItinerary = "[" +
                    "{\"title\":\"15:30 - Ngũ Hành Sơn\",\"content\":\"Xe và hướng dẫn viên đón quý khách tại điểm hẹn, khởi hành tham quan danh thắng Ngũ Hành Sơn - cụm 5 ngọn núi đá vôi nhô lên giữa lòng thành phố. Quý khách sẽ được tham quan ngọn núi Thủy Sơn với hệ thống các hang động huyền ảo như Động Huyền Không, Động Tàng Chơn và chiêm bái các ngôi chùa cổ tự như Chùa Linh Ứng, Chùa Tam Thai. Cảm giác chinh phục các bậc đá và ngắm nhìn toàn cảnh biển Non Nước từ trên cao sẽ là khởi đầu tuyệt vời cho chuyến đi.\"},"
                    +
                    "{\"title\":\"17:30 - Phố cổ Hội An\",\"content\":\"Rời Ngũ Hành Sơn, đoàn tiếp tục di chuyển về Phố cổ Hội An. Khi trời bắt đầu chuyển sang ánh hoàng hôn, Phố Hội trở nên lung linh và thơ mộng lạ thường. Quý khách dạo bước quanh các con phố nhỏ rêu phong, tham quan Chùa Cầu biểu tượng, Nhà cổ Tân Ký - nơi lưu giữ nét kiến trúc giao thoa độc đáo. Không gian hoài cổ cùng hàng ngàn chiếc đèn lồng rực rỡ sắc màu treo khắp lối sẽ mang lại cảm giác bình yên và thư thái tuyệt đối.\"},"
                    +
                    "{\"title\":\"19:00 - Bữa tối đặc sản\",\"content\":\"Thưởng thức bữa tối nồng ấm tại nhà hàng địa phương với các món ăn đặc sản nổi tiếng của Phố Hội như Cao lầu, Mì Quảng, Bánh bao - Bánh vạc. Không gian nhà hàng ấm cúng, đậm chất Quảng Nam cùng sự phục vụ tận tình của nhân viên sẽ làm hài lòng mọi thực khách. Sau bữa tối, quý khách có thể tự do dạo phố, khám phá cuộc sống của người dân địa phương về đêm hoặc thưởng thức trà thảo mộc tại các quán trà mang phong cách cổ xưa.\"},"
                    +
                    "{\"title\":\"20:30 - Thả hoa đăng sông Hoài\",\"content\":\"Quý khách trải nghiệm đi thuyền trên dòng sông Hoài thơ mộng và tự tay thả những chiếc đèn hoa đăng lung linh lấp lánh trên mặt nước. Ánh sáng vàng dịu của nến hòa cùng bóng đèn lồng phản chiếu xuống sông tạo nên khung cảnh huyền ảo như trong truyện cổ tích. Đây là thời điểm tuyệt vời để gửi gắm những lời cầu chúc bình an cho bản thân và gia đình giữa không gian yên ả của dòng sông biểu tượng cho vùng đất nhộn nhịp một thời này.\"},"
                    +
                    "{\"title\":\"21:30 - Kết thúc hành trình\",\"content\":\"Xe đưa quý khách rời Phố cổ Hội An xinh đẹp để trở về lại thành phố Đà Nẵng. Trên đường về, quý khách có thể nghỉ ngơi trên xe và hồi tưởng lại những khoảnh khắc đẹp trong suốt hành trình tham quan Ngũ Hành Sơn hùng vĩ và Hội An lung linh sắc màu. Hướng dẫn viên chia tay đoàn tại điểm đón ban đầu, khép lại một chuyến đi đầy ắp tiếng cười và những trải nghiệm văn hóa ý nghĩa, hẹn gặp lại quý khách trong những hành trình khám phá miền Trung tiếp theo.\"}"
                    +
                    "]";

            String sontraItinerary = "[" +
                    "{\"title\":\"08:00 - Khởi hành lên bán đảo\",\"content\":\"Bắt đầu hành trình chinh phục bán đảo Sơn Trà - lá phổi xanh của Đà Nẵng. Xe và hướng dẫn viên đón quý khách tại điểm hẹn, khởi hành dọc theo con đường biển tuyệt đẹp Võ Nguyên Giáp. Quý khách sẽ được tận hưởng làn gió biển mát rượi và nghe giới thiệu về những điểm dừng chân thú vị trên bán đảo xinh đẹp này.\"},"
                    +
                    "{\"title\":\"09:30 - Viếng Linh Ứng Tự\",\"content\":\"Tham quan chùa Linh Ứng Bãi Bụt, ngôi chùa lớn nhất và đẹp nhất tại Đà Nẵng. Điểm nhấn là tượng Phật Bà Quan Thế Âm cao 67m hướng mắt ra biển Đông che chở cho ngư dân. Quý khách sẽ cảm nhận được sự thanh tịnh, bình yên giữa không gian kiến trúc truyền thống hòa quyện cùng cảnh sắc thiên nhiên hùng vĩ của núi rừng và biển cả.\"},"
                    +
                    "{\"title\":\"11:00 - Khám phá đỉnh Bàn Cờ\",\"content\":\"Tiếp tục hành trình chinh phục đỉnh Bàn Cờ - nơi được mệnh danh là nóc nhà của Đà Nẵng. Quý khách sẽ được thử tài đánh cờ với tiên ông trên đỉnh núi và chiêm ngưỡng toàn cảnh thành phố cùng vịnh Đà Nẵng từ trên cao. Cảm giác chinh phục độ cao và không gian khoáng đạt nơi đây chắc chắn sẽ mang lại những bức ảnh kỷ niệm vô cùng độc đáo.\"},"
                    +
                    "{\"title\":\"14:00 - Check-in Cây Đa Nghìn Năm\",\"content\":\"Ghé thăm cây Đa nghìn năm tuổi với bộ rễ khổng lồ cắm sâu vào lòng đất tạo nên một khung cảnh kỳ ảo như trong những bộ phim thần thoại. Đây là điểm dừng chân lý tưởng để quý khách nghỉ ngơi, hít thở bầu không khí trong lành tuyệt đối của rừng nguyên sinh Sơn Trà và tìm hiểu về hệ sinh thái thực vật đa dạng quý hiếm của vùng bán đảo này.\"},"
                    +
                    "{\"title\":\"16:00 - Tạm biệt Sơn Trà\",\"content\":\"Xe bắt đầu đưa đoàn xuống núi, dọc theo những cung đường uốn lượn để trở về trung tâm thành phố. Một lần nữa quý khách được ngắm nhìn bãi biển Mỹ Khê xinh đẹp và những cây cầu nổi tiếng của Đà Nẵng từ xa. Hướng dẫn viên chia tay đoàn tại điểm đón ban đầu, khép lại một ngày khám phá thiên nhiên Sơn Trà đầy dư âm tốt đẹp và ý nghĩa.\"}"
                    +
                    "]";

            // Update Ba Na Hills
            updateTourWithPremiumContent(
                    "Bà Nà Hills",
                    "Trải nghiệm hành trình đến với Sun World Ba Na Hills - chốn bồng lai tiên cảnh giữa lòng Đà Nẵng. Với độ cao hơn 1.400 mét so với mực nước biển, du khách sẽ được tận hưởng khí hậu bốn mùa trong một ngày độc đáo. Điểm nhấn không thể bỏ qua chính là Cầu Vàng - biểu tượng du lịch quốc tế với đôi bàn tay khổng lồ nâng đỡ dải lụa vàng lấp lánh giữa mây ngàn. Ngoài ra, Làng Pháp với kiến trúc cổ kính, hầm rượu Debay và khu vui chơi Fantasy Park hứa hẹn mang lại những giây phút giải trí tuyệt vời nhất cho gia đình và bạn bè. Đừng quên thưởng thức buffet quốc tế đa dạng và chiêm bái Linh Ứng Tự với tượng Phật Thích Ca uy nghiêm.",
                    "Tận hưởng dịch vụ cáp treo cao cấp, vui chơi không giới hạn và ẩm thực tinh hoa.",
                    banaItinerary,
                    List.of(
                            "Check-in Cầu Vàng - dải lụa vàng giữa đôi bàn tay khổng lồ",
                            "Trải nghiệm hệ thống cáp treo đạt nhiều kỷ lục thế giới",
                            "Khám phá Làng Pháp với kiến trúc châu Âu cổ điển đẳng cấp",
                            "Vui chơi không giới hạn tại Fantasy Park - khu vui chơi trong nhà lớn nhất",
                            "Thưởng thức ẩm thực buffet quốc tế phong phú tại nhà hàng 5 sao"));

            // Update Cu Lao Cham
            updateTourWithPremiumContent(
                    "Cù Lao Chàm",
                    "Khám phá Cù Lao Chàm - cụm đảo xanh mướt được UNESCO công nhận là Khu dự trữ sinh quyển thế giới. Cách phố cổ Hội An chỉ 15km bằng tàu cao tốc, đây là nơi du khách có thể tạm rời xa ồn ào đô thị để hòa mình vào thiên nhiên hoang sơ. Bạn sẽ được tham gia hoạt động lặn ngắm san hô tại Bãi Xếp, nơi có những rặng san hô đa sắc màu và làn nước trong vắt nhìn tận đáy. Tour còn đưa du khách tham quan Chùa Hải Tạng cổ kính và khu bảo tồn biển để hiểu thêm về đời sống người dân biển đảo. Bữa trưa hải sản tươi ngon đậm chất địa phương tại Bãi Ông sẽ là kết thúc hoàn hảo cho hành trình khám phá đại dương này.",
                    "Trải nghiệm sinh thái biển hoang sơ, lặn ngắm san hô và ẩm thực biển tươi sống.",
                    chamItinerary,
                    List.of(
                            "Lặn ngắm san hô chuyên nghiệp tại các rặng san hô tự nhiên đẹp nhất",
                            "Di chuyển bằng tàu cao tốc đời mới đảm bảo an toàn và cực nhanh",
                            "Tham quan các di tích lịch sử lâu đời: Chùa Hải Tạng, Giếng cổ Chăm",
                            "Thưởng thức bữa trưa hải sản đặc sản tươi sống ngay trên đảo",
                            "Tận hưởng không gian nghỉ dưỡng biển xanh cát trắng tại Bãi Ông"));

            // Update Hoi An
            updateTourWithPremiumContent(
                    "Hội An",
                    "Hành trình ngược dòng thời gian về với Phố cổ Hội An - di sản văn hóa thế giới bình yên và quyến phu. Khi hoàng hôn buông xuống, những con phố nhỏ lại bừng sáng bởi hàng ngàn chiếc đèn lồng thủ công rực rỡ sắc màu. Tour sẽ đưa bạn đi qua Chùa Cầu biểu tượng, các hội quán người Hoa uy nghiêm và những ngôi nhà cổ có tuổi đời hàng trăm năm. Du khách còn có cơ hội thả hoa đăng trên dòng sông Hoài thơ mộng, gửi gắm những tâm nguyện bình an. Hội An không chỉ đẹp ở cảnh vật mà còn níu chân thực khách bởi tinh hoa ẩm thực đặc trưng. Đây là một trải nghiệm văn hóa trọn vẹn nhất khi ghé thăm miền Trung.",
                    "Khám phá di sản thế giới lung linh sắc màu đèn lồng và văn hóa truyền thống.",
                    hanoiItinerary,
                    List.of(
                            "Đi bộ tham quan các ngôi nhà cổ hàng trăm năm tuổi",
                            "Trải nghiệm thả hoa đăng lung linh trên dòng sông Hoài thơ mộng",
                            "Thưởng thức ẩm thực đặc sản Hội An chuẩn vị địa phương",
                            "Chiêm ngưỡng vẻ đẹp kỳ ảo của phố đèn lồng rực rỡ về đêm",
                            "Tìm hiểu lịch sử văn hóa sâu sắc qua lời kể của hướng dẫn viên chuyên nghiệp"));

            log.info("Seed: Premium tour content (descriptions & highlights & itineraries) updated.");
        } catch (Exception e) {
            log.error("Premium seeding error: {}", e.getMessage());
        }
    }

    private void updateTourWithPremiumContent(String nameLike, String description, String whyChooseUs, String itinerary,
            List<String> highlights) {
        // Find Tour ID
        List<Map<String, Object>> tours = jdbcTemplate.queryForList("SELECT TourID FROM Tours WHERE TourName LIKE ?",
                "%" + nameLike + "%");
        for (Map<String, Object> t : tours) {
            Long tourId = ((Number) t.get("TourID")).longValue();

            // Update Text Data
            jdbcTemplate.update("UPDATE Tours SET Description = ?, WhyChooseUs = ?, Itinerary = ? WHERE TourID = ?",
                    description, whyChooseUs, itinerary, tourId);

            // Update Highlights (Delete and Re-insert)
            jdbcTemplate.update("DELETE FROM TourHighlights WHERE TourID = ?", tourId);
            for (String h : highlights) {
                jdbcTemplate.update(
                        "INSERT INTO TourHighlights (TourID, Highlight, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tourId, h);
            }

            // Xóa hình ảnh hoạt động của lịch trình trước khi xóa schedules (tránh lỗi
            // Foreign Key)
            jdbcTemplate.update(
                    "DELETE FROM TourActivityImages WHERE ScheduleID IN (SELECT ScheduleID FROM TourSchedules WHERE TourID = ?)",
                    tourId);

            // Update Schedules (Chỉ xóa những schedule chưa có Booking để tránh xung đột
            // khóa ngoại)
            jdbcTemplate.update(
                    "DELETE FROM TourSchedules WHERE TourID = ? AND ScheduleID NOT IN (SELECT DISTINCT ScheduleID FROM Bookings)",
                    tourId);
            for (int i = 1; i <= 4; i++) {
                Integer exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM TourSchedules WHERE TourID = ? AND CAST(StartDate AS DATE) = CAST(DATEADD(day, ?, GETDATE()) AS DATE)",
                        Integer.class, tourId, i);
                if (exists != null && exists > 0)
                    continue;

                jdbcTemplate.update(
                        "INSERT INTO TourSchedules (TourID, StartDate, EndDate, AvailableSlots, MaxSlots, Status, CreatedAt, UpdatedAt) "
                                +
                                "VALUES (?, DATEADD(day, ?, GETDATE()), DATEADD(day, ?, GETDATE()), 20, 20, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tourId, i, i);
            }
        }
    }

    private void ensureDetailedItinerariesForAllTours() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList(
                    "SELECT TourID, TourName, Duration, Itinerary FROM Tours");

            int updated = 0;
            for (Map<String, Object> t : tours) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String tourName = t.get("TourName") == null ? "" : String.valueOf(t.get("TourName"));
                Integer duration = t.get("Duration") == null ? null : ((Number) t.get("Duration")).intValue();
                String itinerary = t.get("Itinerary") == null ? null : String.valueOf(t.get("Itinerary"));

                if (isItinerarySufficient(itinerary)) {
                    continue;
                }

                String generated = generateDetailedItineraryJson(tourName, duration);
                jdbcTemplate.update("UPDATE Tours SET Itinerary = ? WHERE TourID = ?", generated, tourId);
                updated++;
            }

            log.info("Seed: ensured detailed itineraries for all tours. Updated: {}", updated);
        } catch (Exception e) {
            log.error("Ensure itineraries error: {}", e.getMessage());
        }
    }

    private boolean isItinerarySufficient(String itineraryJson) {
        if (itineraryJson == null || itineraryJson.trim().isEmpty())
            return false;
        String trimmed = itineraryJson.trim();
        if (!trimmed.startsWith("["))
            return false;

        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (!node.isArray() || node.size() < 4)
                return false;

            for (JsonNode item : node) {
                String content = item.path("content").asText("");
                if (wordCount(content) < 30)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int wordCount(String text) {
        if (text == null)
            return 0;
        String cleaned = text.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty())
            return 0;
        return cleaned.split(" ").length;
    }

    private String generateDetailedItineraryJson(String tourName, Integer duration) throws Exception {
        String safeName = (tourName == null || tourName.isBlank()) ? "hành trình" : tourName.trim();
        String durationText = (duration == null || duration <= 0) ? "trong ngày" : (duration + " ngày");

        List<Map<String, String>> items = List.of(
                Map.of(
                        "title", "07:30 - Đón khách & khởi hành",
                        "content",
                        "Hướng dẫn viên liên hệ trước, đón quý khách tại điểm hẹn và hỗ trợ sắp xếp chỗ ngồi. Trên xe, chúng tôi giới thiệu tổng quan về "
                                + safeName
                                + ", các điểm dừng chính và lưu ý an toàn. Quý khách nhận nước uống, nghe thuyết minh văn hóa địa phương và khởi động tinh thần cho trải nghiệm "
                                + durationText + " thật trọn vẹn."),
                Map.of(
                        "title", "09:00 - Check-in điểm tham quan đầu tiên",
                        "content",
                        "Đoàn đến điểm tham quan nổi bật mở đầu, tự do chụp ảnh và nghe câu chuyện lịch sử gắn với địa danh. Hướng dẫn viên gợi ý góc chụp đẹp, hỗ trợ sắp xếp nhóm và nhắc nhở bảo vệ môi trường. Quý khách có thời gian thư thả cảm nhận không khí, ghi lại khoảnh khắc và bắt đầu hòa nhịp với nhịp sống bản địa."),
                Map.of(
                        "title", "11:30 - Trải nghiệm địa phương & nghỉ trưa",
                        "content",
                        "Chúng tôi dừng chân tại khu vực tiện nghi để nghỉ ngơi, dùng bữa và nạp năng lượng. Thực đơn ưu tiên đặc sản vùng miền, có lựa chọn nhẹ nhàng cho người lớn tuổi và trẻ em. Hướng dẫn viên chia sẻ thêm về tập quán, ẩm thực và gợi ý hoạt động phù hợp, giúp quý khách vừa thư giãn vừa hiểu sâu hơn về điểm đến."),
                Map.of(
                        "title", "14:00 - Khám phá điểm chính & hoạt động theo chủ đề",
                        "content",
                        "Buổi chiều là phần trải nghiệm đặc sắc nhất của " + safeName
                                + ": tham quan theo lộ trình tối ưu, kết hợp hoạt động tương tác theo chủ đề tour. Quý khách được hướng dẫn từng bước, đảm bảo an toàn và có thời gian tự do mua sắm quà lưu niệm. Chúng tôi luôn giữ nhịp đi hợp lý để mọi người đều thoải mái, không vội vàng nhưng vẫn đủ điểm nổi bật."),
                Map.of(
                        "title", "17:30 - Tổng kết, đưa khách về điểm đón",
                        "content",
                        "Kết thúc lịch trình, đoàn tập trung điểm danh và lên xe trở về. Trên đường về, hướng dẫn viên tổng kết hành trình, hỗ trợ kiểm tra đồ dùng cá nhân và giải đáp thắc mắc. Quý khách nhận hình ảnh gợi ý, lời cảm ơn và hướng dẫn cho lần trải nghiệm tiếp theo. Chúng tôi đưa quý khách về đúng điểm hẹn, kết thúc chuyến đi an toàn và nhiều kỷ niệm."));

        return objectMapper.writeValueAsString(items);
    }

    private void ensureUpcomingSchedulesForAllTours() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList("SELECT TourID FROM Tours");
            int inserted = 0;

            for (Map<String, Object> t : tours) {
                Long tourId = ((Number) t.get("TourID")).longValue();

                for (int i = 1; i <= 4; i++) {
                    Integer exists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM TourSchedules WHERE TourID = ? AND CAST(StartDate AS DATE) = CAST(DATEADD(day, ?, GETDATE()) AS DATE)",
                            Integer.class, tourId, i);
                    if (exists != null && exists > 0)
                        continue;

                    jdbcTemplate.update(
                            "INSERT INTO TourSchedules (TourID, StartDate, EndDate, AvailableSlots, MaxSlots, Status, CreatedAt, UpdatedAt) "
                                    +
                                    "VALUES (?, DATEADD(day, ?, GETDATE()), DATEADD(day, ?, GETDATE()), 20, 20, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            tourId, i, i);
                    inserted++;
                }
            }

            log.info("Seed: ensured upcoming schedules (next 1-4 days). Inserted: {}", inserted);
        } catch (Exception e) {
            log.error("Ensure schedules error: {}", e.getMessage());
        }
    }

    private void ensureHighlightsForAllTours() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList(
                    "SELECT TourID, TourName FROM Tours t WHERE NOT EXISTS (SELECT 1 FROM TourHighlights h WHERE h.TourID = t.TourID)");

            int inserted = 0;
            for (Map<String, Object> t : tours) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String tourName = t.get("TourName") == null ? "hành trình" : String.valueOf(t.get("TourName"));
                String cleanName = tourName.toLowerCase().replace("tour", "").trim();

                List<String> highlights = List.of(
                        "Khám phá trọn vẹn những điểm đến đặc sắc và nổi bật nhất của " + cleanName,
                        "Trải nghiệm dịch vụ du lịch trọn gói chuyên nghiệp với tiêu chuẩn 5 sao",
                        "Thưởng thức tinh hoa ẩm thực đặc trưng bản địa cùng chuyên gia",
                        "Lịch trình được tối ưu hóa đặc biệt, đảm bảo sự thoải mái và an toàn tối đa",
                        "Đồng hành cùng đội ngũ hướng dẫn viên am hiểu, hỗ trợ chụp ảnh check-in tuyệt đẹp");

                for (String h : highlights) {
                    jdbcTemplate.update(
                            "INSERT INTO TourHighlights (TourID, Highlight, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            tourId, h);
                }
                inserted++;
            }

            log.info("Seed: ensured highlights for all tours. Inserted for {} tours", inserted);
        } catch (Exception e) {
            log.error("Ensure highlights error: {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        try {
            String[] adminEmails = { "admin@dana.com", "admin@gmail.com" };
            // BCrypt hash cho "123456"
            String passwordHash = "$2a$10$7vj26Aptw/yE0uT/8f6BGe.1e.W0U9WfNn0/2fV9rUfB5W1N8yD9w";

            for (String email : adminEmails) {
                List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT UserID FROM Users WHERE Email = ?",
                        email);

                if (users.isEmpty()) {
                    jdbcTemplate.update(
                            "INSERT INTO Users (Email, FullName, PasswordHash, Role, IsActive, CreatedAt, UpdatedAt) VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            email, "Admin Account", passwordHash, "ADMIN");
                    log.info("Seed: Created admin user {}", email);
                } else {
                    jdbcTemplate.update("UPDATE Users SET Role = 'ADMIN', IsActive = 1 WHERE Email = ?", email);
                    log.info("Seed: Updated admin user {} to ADMIN role", email);
                }
            }
        } catch (Exception e) {
            log.error("Admin seeding error: {}", e.getMessage());
        }
    }

    private void seedTourItineraryDays() {
        try {
            // Delete old itinerary days first to prevent duplicate key violations and ensure clean sync
            try {
                jdbcTemplate.execute("DELETE FROM tour_itinerary_day");
            } catch (Exception e) {
                log.warn("Could not delete old itinerary days: {}", e.getMessage());
            }

            List<Map<String, Object>> tours = jdbcTemplate.queryForList("SELECT TourID, TourName FROM Tours");
            int total = 0;

            for (Map<String, Object> t : tours) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String tourName = t.get("TourName") == null ? "" : String.valueOf(t.get("TourName"));

                String title = "Ngày 1: Khám phá địa điểm";
                String meals = "Tự túc";
                String accommodation = "Không bao gồm";
                String trans = "Xe du lịch";
                String desc = "Bắt đầu hành trình du lịch tham quan khám phá các thắng cảnh nổi tiếng...";

                if (tourName.contains("Bà Nà")) {
                    title = "Hành trình chinh phục đỉnh Bà Nà Hills và check-in Cầu Vàng";
                    meals = "Buffet trưa 5 sao";
                    accommodation = "Không bao gồm";
                    trans = "Xe du lịch đời mới";
                    desc = "Đón khách từ trung tâm Đà Nẵng khởi hành đi Bà Nà, trải nghiệm cáp treo ngắm toàn cảnh rừng nguyên sinh, tham quan Cầu Vàng nổi tiếng, làng Pháp cổ kính và ăn buffet quốc tế cực kỳ thịnh soạn.";
                } else if (tourName.contains("Hội An")) {
                    title = "Khám phá phố cổ Hội An lung linh đèn lồng và thả hoa đăng sông Hoài";
                    meals = "Ăn tối đặc sản Hội An";
                    accommodation = "Tự túc";
                    trans = "Xe điện du lịch";
                    desc = "Dạo bước qua các con phố cổ ngói rêu phong, tham quan Chùa Cầu, hội quán Phước Kiến, thưởng thức đặc sản Cao Lầu, cơm gà và đi thuyền thả hoa đăng ước nguyện trên sông Hoài thơ mộng.";
                } else if (tourName.contains("Cù Lao Chàm")) {
                    title = "Cano cao tốc lướt sóng ra đảo Cù Lao Chàm lặn ngắm san hô";
                    meals = "Buffet hải sản tươi ngon";
                    accommodation = "Không bao gồm";
                    trans = "Cano cao tốc";
                    desc = "Lên cano cao tốc vượt sóng ra đảo, khám phá giếng cổ Chăm và chùa Hải Tạng, lặn ngắm san hô tự nhiên tại Bãi Xếp và thưởng thức tiệc hải sản tươi sống chất lượng tại Bãi Ông.";
                } else if (tourName.contains("Huế")) {
                    title = "Hành trình di sản Cố đô Huế trầm mặc và cổ kính";
                    meals = "Ăn trưa đặc sản Huế";
                    accommodation = "Không bao gồm";
                    trans = "Xe du lịch";
                    desc = "Di chuyển qua hầm Hải Vân đến Huế, tham quan Đại Nội Hoàng Cung của 13 vị vua triều Nguyễn, viếng chùa Thiên Mụ linh thiêng và lăng tẩm Khải Định với kiến trúc độc đáo.";
                } else if (tourName.contains("Ngũ Hành Sơn") || tourName.contains("Sơn Trà")) {
                    title = "Chiêm bái tâm linh chùa Linh Ứng Sơn Trà và danh thắng Ngũ Hành Sơn";
                    meals = "Ăn tối bánh tráng thịt heo";
                    accommodation = "Không bao gồm";
                    trans = "Xe du lịch";
                    desc = "Tham quan chùa Linh Ứng trên bán đảo Sơn Trà với tượng Phật Bà Quan Âm cao 67m, chinh phục các hang động huyền bí tại Ngũ Hành Sơn và tham quan làng đá mỹ nghệ Non Nước.";
                } else if (tourName.contains("Mỹ Sơn")) {
                    title = "Khám phá thung lũng di sản Thánh địa Mỹ Sơn vương triều Chăm Pa";
                    meals = "Ăn trưa bê thui Cầu Mống";
                    accommodation = "Không bao gồm";
                    trans = "Xe du lịch";
                    desc = "Khám phá tổ hợp đền tháp cổ rêu phong xây dựng bằng kỹ thuật xếp gạch độc đáo từ thế kỷ IV, xem biểu diễn múa Apsara của các vũ nữ Chăm chuyên nghiệp.";
                } else if (tourName.contains("Food") || tourName.contains("Ẩm thực")) {
                    title = "Khám phá thiên đường ẩm thực đường phố Đà Nẵng về đêm";
                    meals = "Ăn vặt, Mỳ Quảng, bánh xèo, nem lụi, chè sầu";
                    accommodation = "Tự túc";
                    trans = "Xe máy / Đi bộ";
                    desc = "Trải nghiệm đi chợ đêm, thưởng thức các món ăn vặt độc đáo như ốc hút, bánh tráng kẹp, Mỳ Quảng ếch và chè sầu Liên nức tiếng cùng hướng dẫn viên bản địa.";
                } else if (tourName.contains("Dừa Bảy Mẫu")) {
                    title = "Khám phá rừng dừa Bảy Mẫu miền Tây thu nhỏ giữa miền Trung";
                    meals = "Ăn trưa cơm quê";
                    accommodation = "Không bao gồm";
                    trans = "Xe điện / Thúng chai";
                    desc = "Ngồi thúng chai len lỏi qua rạch dừa nước xanh mát, xem biểu diễn quay thúng chai điệu nghệ của các nghệ nhân địa phương và tham gia các trò chơi dân gian thú vị.";
                } else if (tourName.contains("Mỹ Khê")) {
                    title = "Tận hưởng không khí trong lành tại bãi biển Mỹ Khê và phố đêm";
                    meals = "Ăn tối hải sản";
                    accommodation = "Khách sạn 3 sao";
                    trans = "Xe du lịch";
                    desc = "Tắm biển tự do tại một trong những bãi biển quyến rũ nhất hành tinh, thưởng thức bữa tối hải sản tươi sống và dạo chơi phố đêm sôi động sát bờ biển.";
                }

                jdbcTemplate.update(
                        "INSERT INTO tour_itinerary_day (tour_id, day_number, title, description, accommodation, meals, transportation, CreatedAt, UpdatedAt) VALUES (?, 1, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tourId, title, desc, accommodation, meals, trans);
                total++;
            }
            log.info("Seed: Inserted {} tour itinerary days to support meals/accommodations", total);
        } catch (Exception e) {
            log.error("Tour itinerary days seeding error: {}", e.getMessage());
        }
    }

    private void seedWhyChooseUsForAllTours() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList("SELECT TourID, TourName FROM Tours");
            int updated = 0;

            for (Map<String, Object> t : tours) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String name = t.get("TourName") == null ? "" : String.valueOf(t.get("TourName"));

                String whyChooseText = "";

                if (name.contains("Bà Nà")) {
                    whyChooseText = "Bà Nà Hills là điểm đến không thể bỏ qua tại Đà Nẵng nhờ khí hậu mát mẻ quanh năm và cảnh quan tựa chốn bồng lai tiên cảnh. Khi lựa chọn tour của Dana, quý khách sẽ được trải nghiệm trọn gói dịch vụ chuẩn 5 sao bao gồm xe đưa đón đời mới chất lượng cao cùng hướng dẫn viên bản địa am hiểu sâu sắc. Điểm nhấn của tour là cơ hội check-in Cầu Vàng - dải lụa vàng lấp lánh giữa đôi bàn tay khổng lồ rêu phong vươn giữa mây ngàn. Ngoài ra, quý khách sẽ được đi cáp treo đạt nhiều kỷ lục thế giới, dạo bước trong làng Pháp cổ kính đậm chất châu Âu thế kỷ 19, tham quan vườn hoa Le Jardin D'Amour và hầm rượu Debay trăm tuổi. Đặc biệt, tour đã bao gồm vé vui chơi không giới hạn tại Fantasy Park và bữa trưa buffet quốc tế thượng hạng với hơn 100 món ăn Á-Âu phong phú tại nhà hàng sang trọng. Đây là lựa chọn hoàn hảo nhất cho kỳ nghỉ của bạn!";
                } else if (name.contains("Hội An")) {
                    whyChooseText = "Phố cổ Hội An mang vẻ đẹp yên bình, trầm mặc và lung linh sắc màu di sản văn hóa thế giới. Chọn tour Hội An của chúng tôi là cơ hội tốt nhất để bạn cảm nhận linh hồn của mảnh đất di sản này một cách trọn vẹn và sâu sắc nhất. Tour được thiết kế với thời gian khởi hành hợp lý vào buổi chiều để quý khách có thể đón hoàng hôn buông xuống trên dòng sông Hoài thơ mộng. Hướng dẫn viên chuyên nghiệp của chúng tôi sẽ đưa bạn đi bộ qua những con phố nhỏ rêu phong, tham quan Chùa Cầu có lịch sử hơn 400 năm, các nhà cổ lâu đời như Tân Ký hay Phùng Hưng, cùng hội quán Phúc Kiến tinh xảo. Bạn sẽ được thưởng thức bữa tối ấm cúng với những món đặc sản địa phương nức tiếng như Cao lầu, mì Quảng hay cơm gà. Trải nghiệm đặc sắc nhất của tour chính là đi thuyền trên sông Hoài và tự tay thả những chiếc hoa đăng lung linh lấp lánh để gửi gắm ước nguyện bình an, may mắn cho gia đình. Hãy đồng hành cùng chúng tôi để có hành trình văn hóa đáng nhớ!";
                } else if (name.contains("Cù Lao Chàm")) {
                    whyChooseText = "Cù Lao Chàm là khu dự trữ sinh quyển thế giới được UNESCO công nhận, nổi tiếng với những bãi biển hoang sơ và hệ sinh thái đại dương phong phú. Quyết định chọn tour Cù Lao Chàm của Dana là lựa chọn sáng suốt nhất để trải nghiệm một ngày nghỉ ngơi đúng nghĩa giữa biển xanh cát trắng. Chúng tôi mang đến dịch vụ cano cao tốc đời mới cực kỳ an toàn và nhanh chóng, giúp bạn tiết kiệm thời gian di chuyển. Điểm nổi bật nhất của chuyến đi là hoạt động lặn ngắm san hô chuyên nghiệp tại Bãi Xếp. Quý khách sẽ được hòa mình vào làn nước biển trong vắt nhìn thấy đáy để ngắm nhìn những rặng san hô tự nhiên đa sắc màu và các đàn cá bơi lội xung quanh. Hướng dẫn viên bản địa sẽ tận tình đưa đoàn tham quan các di tích lịch sử như chùa cổ Hải Tạng và giếng cổ Chăm Pa. Cuối cùng, quý khách sẽ được thưởng thức tiệc buffet hải sản tươi ngon tuyệt vời được đánh bắt trực tiếp trong ngày tại nhà hàng ven biển Bãi Ông lộng gió. Hãy liên hệ ngay hôm nay!";
                } else if (name.contains("Phú Quốc")) {
                    whyChooseText = "Đảo ngọc Phú Quốc là thiên đường nghỉ dưỡng nhiệt đới với những bãi biển cát trắng mịn màng và làn nước ấm áp quanh năm. Chọn tour Phú Quốc của chúng tôi, quý khách sẽ được tận hưởng kỳ nghỉ trọn vẹn tại các resort cao cấp sát biển, có xe đưa đón đời mới suốt hành trình. Chương trình tour độc đáo đưa bạn khám phá thế giới giải trí đỉnh cao VinWonders, vườn thú bán hoang dã Safari lớn nhất Việt Nam và trải nghiệm đi cáp treo vượt biển Hòn Thơm đạt kỷ lục Guinness thế giới. Bên cạnh đó, bạn sẽ tham gia hoạt động câu cá, lặn ngắm san hô tại hòn Mây Rút, hòn Móng Tay và thưởng thức hải sản tươi sống vô cùng thơm ngon. Hướng dẫn viên am hiểu bản địa sẽ đồng hành và chăm sóc quý khách chu đáo suốt chuyến đi. Đây chắc chắn là chuyến đi đáng giá từng xu!";
                } else if (name.contains("Hạ Long")) {
                    whyChooseText = "Vịnh Hạ Long - kỳ quan thiên nhiên thế giới với hàng ngàn đảo đá vôi kỳ vĩ vươn lên từ mặt nước xanh ngọc bích. Trải nghiệm tour Hạ Long cùng chúng tôi, quý khách sẽ được du ngoạn trên những chiếc du thuyền sang trọng chuẩn 5 sao, thưởng thức ẩm thực hải sản tươi ngon thượng hạng và tham gia các hoạt động ngoài trời cực kỳ lý thú như chèo thuyền kayak qua các hang luồn, tắm biển tại đảo Ti Tốp và leo núi ngắm toàn cảnh vịnh từ trên cao. Hướng dẫn viên chuyên nghiệp sẽ đưa đoàn tham quan Hang Sửng Sốt - một trong những hang động đẹp và rộng lớn nhất vịnh Hạ Long. Dịch vụ chăm sóc khách hàng chu đáo và chất lượng phục vụ đỉnh cao của chúng tôi cam kết mang lại cho bạn những phút giây thư giãn tuyệt đối và những tấm ảnh check-in sang chảnh nhất!";
                } else if (name.contains("Sa Pa") || name.contains("Sapa")) {
                    whyChooseText = "Sa Pa - thị trấn trong mây với phong cảnh núi non hùng vĩ, những thửa ruộng bậc thang tuyệt mỹ và bản sắc văn hóa phong phú của các dân tộc thiểu số miền núi phía Bắc. Tour Sa Pa của Dana sẽ đưa bạn chinh phục đỉnh Fansipan - nóc nhà Đông Dương bằng hệ thống cáp treo hiện đại ba dây đạt nhiều kỷ lục thế giới. Bạn sẽ được đi bộ trekking qua các bản làng mộc mạc như Cát Cát, Tả Van, tìm hiểu phong tục tập quán độc đáo của người H'Mông, người Dao đỏ và thưởng thức các món ăn đặc sản ấm cúng như thắng cố, thịt trâu gác bếp, lẩu cá hồi giữa cái lạnh se se của vùng cao. Chúng tôi cam kết dịch vụ lưu trú chất lượng cao, xe limousine êm ái suốt tuyến và hướng dẫn viên nhiệt tình, chu đáo nhất!";
                } else if (name.contains("Đà Lạt")) {
                    whyChooseText = "Đà Lạt - thành phố ngàn hoa thơ mộng với khí hậu mát mẻ quanh năm là điểm trốn nóng lý tưởng cho mọi du khách. Lựa chọn tour Đà Lạt của chúng tôi, bạn sẽ được check-in những địa điểm hot nhất hiện nay như thung lũng Tình Yêu, hồ Vô Cực, cổng trời Bali và dạo bước quanh hồ Xuân Hương lãng mạn. Tour đã bao gồm xe du lịch êm ái đưa đón suốt hành trình, hướng dẫn viên nhiệt tình hỗ trợ chụp ảnh check-in và khách sạn tiện nghi tọa lạc ngay trung tâm thành phố. Quý khách còn được thưởng thức buffet rau nổi tiếng, trải nghiệm hái dâu tây tại vườn và khám phá ẩm thực phong phú tại chợ đêm Đà Lạt. Đây chính là hành trình đem lại sự thư thái và ngọt ngào nhất cho kỳ nghỉ của bạn!";
                } else if (name.contains("Nha Trang")) {
                    whyChooseText = "Nha Trang là thành phố biển năng động sở hữu một trong những vịnh biển đẹp nhất thế giới. Tour Nha Trang của chúng tôi đưa bạn đi du ngoạn hệ thống đảo yến hoang sơ, trải nghiệm lặn biển ngắm san hô bằng bình khí tại Hòn Mun, tham gia các trò chơi cảm giác mạnh trên biển như dù bay, mô tô nước tại Bãi Tranh. Bạn còn được vui chơi thỏa thích tại thiên đường giải trí VinWonders Nha Trang với show diễn triệu đô Tata Show hoành tráng. Tour đã bao gồm dịch vụ xe đưa đón chất lượng cao, bữa trưa hải sản tươi sống phong phú tại làng chài và cơ hội tắm bùn khoáng nóng thư giãn phục hồi sức khỏe cực kỳ tốt. Hãy chọn Dana để tận hưởng kỳ nghỉ hè sôi động và đầy hứng khởi!";
                } else if (name.contains("Huế")) {
                    whyChooseText = "Cố đô Huế - vùng đất di sản mang vẻ đẹp trầm mặc, thơ mộng bên dòng sông Hương hiền hòa. Chọn tour Huế của chúng tôi, quý khách sẽ có hành trình văn hóa lịch sử vô cùng sâu sắc khi được hướng dẫn viên thuyết minh chi tiết về triều đại nhà Nguyễn tại Đại Nội Hoàng Cung, chiêm bái chùa Thiên Mụ cổ kính và khám phá kiến trúc lăng tẩm uy nghi của vua Khải Định hay Minh Mạng. Tour bao gồm xe du lịch máy lạnh êm ái đi qua cung đường biển tuyệt đẹp, bữa trưa đậm đà hương vị ẩm thực Huế với bún bò, bánh lọc, bánh nậm và trải nghiệm nghe ca Huế trên thuyền rồng sông Hương về đêm. Đây là lựa chọn lý tưởng cho những ai yêu mến giá trị truyền thống dân tộc!";
                } else if (name.contains("Hà Nội")) {
                    whyChooseText = "Thủ đô Hà Nội nghìn năm văn hiến mang nét đẹp cổ kính, thanh lịch đặc trưng của 36 phố phường. Tour Hà Nội của chúng tôi sẽ đưa bạn đi viếng Lăng Bác trang nghiêm, tham quan Chùa Một Cột với kiến trúc hoa sen độc đáo, Văn Miếu Quốc Tử Giám - trường đại học đầu tiên của Việt Nam và dạo bước quanh hồ Hoàn Kiếm ngắm tháp Rùa rêu phong. Bạn sẽ được thưởng thức những món ẩm thực tinh tế nức tiếng như phở Lý Quốc Sư, bún chả Obama, bánh tôm Hồ Tây và cà phê trứng thơm ngậy. Chúng tôi đảm bảo dịch vụ đưa đón an toàn, hướng dẫn viên lịch sự am hiểu sâu rộng lịch sử thủ đô, mang lại cho bạn chuyến đi đầy ý nghĩa và cảm xúc.";
                } else if (name.contains("Ngũ Hành Sơn") || name.contains("Sơn Trà")) {
                    whyChooseText = "Hành trình kết hợp tuyệt vời giữa vẻ đẹp núi non kỳ vĩ và tâm linh sâu sắc tại thành phố đáng sống Đà Nẵng. Quý khách sẽ được tham quan bán đảo Sơn Trà hoang sơ, viếng chùa Linh Ứng Bãi Bụt chiêm bái tượng Phật Bà Quan Thế Âm cao 67m hướng biển che chở cho người dân. Tiếp theo là khám phá danh thắng Ngũ Hành Sơn với hệ thống hang động huyền bí như động Huyền Không, động Tàng Chơn và ngắm nhìn toàn cảnh thành phố từ vọng giang đài. Tour đã bao gồm trọn gói xe đưa đón, hướng dẫn viên năng động, vé tham quan, bữa tối đặc sản bánh tráng cuốn thịt heo hai đầu da nổi tiếng và xem nghệ thuật tạc đá tinh xảo tại làng đá Non Nước.";
                } else if (name.contains("Dừa Bảy Mẫu")) {
                    whyChooseText = "Rừng dừa Bảy Mẫu Cẩm Thanh được ví như một miền Tây Nam Bộ thu nhỏ ngay giữa lòng miền Trung. Khi lựa chọn tour của chúng tôi, quý khách sẽ được trải nghiệm ngồi trên những chiếc thúng chai bập bềnh len lỏi qua rạch dừa nước xanh mát vô cùng yên bình. Điểm nhấn hấp dẫn nhất là màn biểu diễn quay thúng chai kịch tính và điêu luyện của các nghệ nhân địa phương, đem lại tiếng cười sảng khoái cho du khách. Quý khách còn được tham gia các hoạt động dân gian vui nhộn như câu cua, đua thúng và thưởng thức bữa trưa cơm quê đậm chất dân dã ngay tại khu sinh thái. Đây là tour dã ngoại tuyệt vời dành cho gia đình và tập thể teambuilding!";
                } else if (name.contains("Mỹ Khê")) {
                    whyChooseText = "Bãi biển Mỹ Khê Đà Nẵng từng được tạp chí Forbes vinh danh là một trong sáu bãi biển quyến rũ nhất hành tinh nhờ bãi cát trắng mịn, sóng biển ôn hòa và làn nước ấm quanh năm. Lựa chọn trải nghiệm tour phố đêm Mỹ Khê của chúng tôi, quý khách sẽ được tự do tắm biển thư giãn, tham gia các hoạt động thể thao bãi biển sôi động và thưởng thức bữa tiệc hải sản tươi sống cực kỳ thơm ngon tại nhà hàng lộng gió sát bờ biển. Hướng dẫn viên sẽ đưa bạn đi dạo chợ đêm Mỹ Khê mua sắm quà lưu niệm và ngắm nhìn thành phố Đà Nẵng lung linh ánh đèn về đêm. Một hành trình nghỉ ngơi và thư giãn tuyệt vời sau những ngày làm việc mệt mỏi!";
                } else {
                    whyChooseText = "Chuyến đi này được Dana thiết kế tỉ mỉ nhằm đem lại trải nghiệm du lịch trọn vẹn và đáng nhớ nhất cho quý khách. Chúng tôi cam kết mức giá cạnh tranh nhất thị trường cùng chất lượng dịch vụ chuẩn 5 sao vượt trội. Xe đưa đón đời mới máy lạnh êm ái, hướng dẫn viên chuyên nghiệp nhiệt tình đồng hành suốt tuyến, hỗ trợ chụp ảnh check-in tuyệt đẹp cho đoàn. Lịch trình tham quan được tối ưu hóa hợp lý, vừa đủ thời gian thư giãn vừa khám phá được những nét độc đáo nhất của điểm đến. Quyết định lựa chọn chúng tôi chắc chắn sẽ đem lại sự hài lòng tuyệt đối và những kỷ niệm tuyệt vời cho bạn và gia đình!";
                }

                jdbcTemplate.update("UPDATE Tours SET WhyChooseUs = ? WHERE TourID = ?", whyChooseText, tourId);
                updated++;
            }
            log.info("Seed: Updated WhyChooseUs description with 150+ words for {} tours", updated);
        } catch (Exception e) {
            log.error("Error updating WhyChooseUs: {}", e.getMessage());
        }
    }
}
