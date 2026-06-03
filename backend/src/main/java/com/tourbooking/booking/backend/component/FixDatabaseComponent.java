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

        try {
            // 1. Seed Admin User
            seedAdminUser();

            // 2. Seed Premium Content (Commented out to speed up startup)
            // seedPremiumTourData();

            // 3. Ensure ALL tours have data (Commented out to speed up startup)
            // ensureDetailedItinerariesForAllTours();
            // ensureUpcomingSchedulesForAllTours();
            // ensureHighlightsForAllTours();

            // 4. Seed FAQs (Commented out to speed up startup)
            // seedFaqs();

        } catch (Exception e) {
            log.error("Initialization error (continuing app startup): {}", e.getMessage());
        }

        log.info("--- DATABASE INITIALIZATION COMPLETED ---");
    }

    private void seedFaqs() {
        try {
            // Seed Global FAQs
            Integer globalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TourFaqs WHERE TourID IS NULL", Integer.class);
            if (globalCount != null && globalCount == 0) {
                String[][] globalFaqs = {
                    {"Chính sách hoàn/hủy tour như thế nào?", "Nếu hủy trước 7 ngày khởi hành, quý khách được hoàn 100%. Nếu hủy trước 3-6 ngày, hoàn 50%. Trong vòng 48 giờ trước chuyến đi không được hoàn tiền. Quy định áp dụng trừ các trường hợp bất khả kháng như thời tiết xấu hoặc dịch bệnh."},
                    {"Trẻ em có được miễn phí hoặc giảm giá không?", "Trẻ em cao dưới 1 mét được miễn phí 100%. Trẻ em từ 1m đến 1.4m tính 70% giá tour người lớn. Từ 1.4m trở lên tính như người lớn."},
                    {"Giá tour đã bao gồm phí tham quan chưa?", "Giá tour đa số đều bao gồm xe đưa đón, hướng dẫn viên, vé tham quan các điểm trong chương trình và bữa ăn (tùy tour). Vui lòng xem kỹ mục Bao gồm/Không bao gồm ở chuyến đi cụ thể."},
                    {"Làm sao để tôi nhận được xác nhận đặt tour?", "Khi quý khách đặt tour và thanh toán thành công (hoặc chọn thanh toán sau), hệ thống sẽ hiển thị mã giao dịch và trạng thái đơn tại mục 'Tour đã đặt'. Quý khách cũng có thể chụp màn hình giao dịch lúc thanh toán xong."}
                };
                for (String[] faq : globalFaqs) {
                    jdbcTemplate.update("INSERT INTO TourFaqs (Question, Answer, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", faq[0], faq[1]);
                }
                log.info("Seed: Inserted Global FAQs");
            }

            // Seed Tour-specific FAQs for all tours (if they don't have any)
            List<Map<String, Object>> toursToSeed = jdbcTemplate.queryForList(
                "SELECT TourID, TourName FROM Tours t WHERE NOT EXISTS (SELECT 1 FROM TourFaqs f WHERE f.TourID = t.TourID)"
            );
            
            for (Map<String, Object> t : toursToSeed) {
                Long tourId = ((Number) t.get("TourID")).longValue();
                String tourName = t.get("TourName") == null ? "này" : String.valueOf(t.get("TourName"));
                
                String[][] customFaqs = {
                    {"Tour " + tourName + " có phù hợp với người cao tuổi không?", "Lịch trình được thiết kế khá nhẹ nhàng và có nhiều điểm dừng chân nghỉ ngơi. Tuy nhiên, xin lưu ý quý khách cao tuổi nên mang theo thuốc cá nhân và báo trước với HDV để được chăm sóc tốt nhất."},
                    {"Nên mang theo gì khi tham gia trải nghiệm này?", "Quý khách nên chuẩn bị giày đi bộ thoải mái, áo khoác mỏng, mũ nón, kem chống nắng và điện thoại/máy ảnh đầy pin để ghi lại những khoảnh khắc đẹp."}
                };
                for (String[] faq : customFaqs) {
                    jdbcTemplate.update("INSERT INTO TourFaqs (TourID, Question, Answer, CreatedAt, UpdatedAt) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", tourId, faq[0], faq[1]);
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
                "{\"title\":\"08:00 - Khởi hành đón khách\",\"content\":\"Bắt đầu hành trình khám phá Bà Nà Hills đầy thú vị. Đội ngũ hướng dẫn viên chuyên nghiệp và tài xế nhiệt tình của chúng tôi sẽ đón quý khách tại điểm hẹn trung tâm Đà Nẵng hoặc tại sảnh khách sạn bằng xe du lịch đời mới, tiện nghi. Quý khách sẽ được giới thiệu về lịch trình chi tiết và những lưu ý cần thiết để có một chuyến đi trọn vẹn và thoải mái nhất.\"}," +
                "{\"title\":\"09:30 - Check-in Cầu Vàng\",\"content\":\"Quý khách di chuyển bằng hệ thống cáp treo đạt nhiều kỷ lục thế giới để đến với biểu tượng Cầu Vàng (Golden Bridge). Tại đây, bạn sẽ được tự do chụp hình check-in với đôi bàn tay khổng lồ nâng đỡ dải lụa vàng giữa mây ngàn hung vĩ. Hướng dẫn viên sẽ hỗ trợ quý khách có được những góc ảnh đẹp nhất và kể về ý tưởng kiến trúc độc đáo của công trình mang tầm quốc tế này.\"}," +
                "{\"title\":\"12:00 - Ăn trưa Buffet\",\"content\":\"Thưởng thức bữa trưa buffet đẳng cấp tại nhà hàng sang trọng trên đỉnh Bà Nà. Thực đơn vô cùng phong phú với hơn 100 món ăn từ ẩm thực Việt Nam truyền thống đến tinh hoa ẩm thực Á - Âu hiện đại. Không gian nhà hàng rộng rãi, thoáng mát cùng sự phục vụ tận tâm sẽ giúp quý khách nạp lại năng lượng tuyệt vời sau buổi sáng tham quan sôi động.\"}," +
                "{\"title\":\"13:30 - Fantasy Park & Làng Pháp\",\"content\":\"Tự do khám phá khu vui chơi trong nhà Fantasy Park lớn nhất Việt Nam với nhiều trò chơi hấp dẫn cho mọi lứa tuổi. Sau đó, quý khách dạo bước quanh Làng Pháp (French Village) - nơi tái hiện một châu Âu thu nhỏ đầy lãng mạn với những lâu đài cổ kính, quảng trường thơ mộng và những con phố lát đá nghệ thuật, mang lại cảm giác như đang lạc giữa lòng nước Pháp cổ xưa.\"}," +
                "{\"title\":\"16:00 - Tạm biệt Bà Nà\",\"content\":\"Tập trung tại ga cáp treo để bắt đầu hành trình xuống núi. Một lần nữa quý khách được ngắm nhìn toàn cảnh rừng nguyên sinh Sơn Trà và vịnh Đà Nẵng từ trên cao trong ánh hoàng hôn dịu dàng. Xe và hướng dẫn viên sẽ đưa quý khách trở về điểm đón ban đầu, kết thúc tốt đẹp hành trình khám phá chốn bồng lai tiên cảnh và hẹn gặp lại trong những chuyến đi tiếp theo.\"}" +
                "]";

            String hanoiItinerary = "[" +
                "{\"title\":\"15:30 - Khởi hành đi Hội An\",\"content\":\"Xe và hướng dẫn viên đón đoàn tại điểm hẹn, bắt đầu hành trình tham quan Phố cổ Hội An - Di sản văn hóa thế giới. Trên đường đi, quý khách sẽ được nghe kể về lịch sử giao thương sầm uất của thương cảng Hội An xưa, những nét văn hóa giao thoa độc đáo giữa Việt Nam, Nhật Bản và Trung Hoa vẫn còn lưu giữ vẹn nguyên cho đến ngày nay.\"}," +
                "{\"title\":\"17:00 - Tham quan Phố Cổ\",\"content\":\"Dạo bước trên những con phố nhỏ rêu phong, chiêm bái Chùa Cầu biểu tượng có tuổi đời hơn 400 năm. Tiếp tục tham quan Hội quán Phước Kiến, Nhà cổ Tân Ký với kiến trúc chạm trổ tinh xảo. Quý khách sẽ được hòa mình vào không gian yên bình, ngắm nhìn những chiếc đèn lồng thủ công rực rỡ sắc màu được treo khắp các hiên nhà, tạo nên một vẻ đẹp lung linh và đầy hoài niệm.\"}," +
                "{\"title\":\"18:30 - Ẩm thực Hội An\",\"content\":\"Thưởng thức bữa tối nồng ấm với các món ăn đặc sản nổi tiếng như Cao lầu, Mì Quảng, Bánh bao - Bánh vạc. Mỗi món ăn đều mang hương vị đặc trưng riêng biệt của vùng đất Quảng Nam, được chế biến từ những nguyên liệu tươi ngon nhất. Không gian nhà hàng ấm cúng bên dòng sông Hoài thơ mộng sẽ mang lại cảm giác thư thái và ngon miệng cho mỗi thực khách.\"}," +
                "{\"title\":\"20:00 - Thả hoa đăng sông Hoài\",\"content\":\"Trải nghiệm đi thuyền trên dòng sông Hoài thơ mộng và tự tay thả những chiếc đèn hoa đăng lung linh. Quý khách có thể gửi gắm những tâm nguyện, ước mong bình an và may mắn theo dòng nước. Ánh sáng của hàng ngàn chiếc đèn lồng phản chiếu xuống mặt nước hòa cùng âm thanh hò khoan xứ Quảng sẽ tạo nên những kỷ niệm tuyệt đẹp và sâu lắng trong hành trình khám phá đêm Hội An.\"}," +
                "{\"title\":\"21:00 - Trở về Đà Nẵng\",\"content\":\"Tập trung lên xe và bắt đầu hành trình trở về lại thành phố Đà Nẵng năng động. Quý khách có thời gian nghỉ ngơi và chia sẻ những ấn tượng đẹp về Phố Hội với bạn bè sau một chuyến đi ý nghĩa. Hướng dẫn viên chân thành cảm ơn và chào tạm biệt đoàn tại điểm đón ban đầu, khép lại chương trình tour Hội An đầy màu sắc văn hóa và cảm xúc khó quên.\"}" +
                "]";

            String chamItinerary = "[" +
                "{\"title\":\"08:00 - Đón khách & Ra cảng\",\"content\":\"Xe đón quý khách tại khách sạn và di chuyển đến cảng Cửa Đại. Tại đây, quý khách sẽ được hướng dẫn làm thủ tục lên tàu cao tốc đời mới để bắt đầu hành trình vượt sóng ra đảo Cù Lao Chàm. Cảm giác lướt đi trên mặt biển bao la và làn gió mát lành sẽ khiến mọi mệt mỏi tan biến, mang lại sự phấn khích cho một ngày khám phá biển đảo hoang sơ phía trước.\"}," +
                "{\"title\":\"09:30 - Lặn ngắm san hô\",\"content\":\"Tàu đưa đoàn đến Bãi Xếp hoặc Bãi Ông để thực hiện hoạt động lặn ngắm san hô (snorkeling). Với làn nước trong xanh nhìn tận đáy, quý khách sẽ được chiêm ngưỡng thế giới đại dương rực rỡ với hàng trăm loài san hô đa dạng và những đàn cá nhỏ đầy màu sắc bơi lội xung quanh. Đội ngũ cứu hộ chuyên nghiệp sẽ luôn túc trực để đảm bảo an toàn tuyệt đối cho mọi thành viên.\"}," +
                "{\"title\":\"12:00 - Buffet hải sản tươi\",\"content\":\"Thưởng thức bữa trưa hải sản tươi sống được chế biến theo phong cách dân dã của ngư dân đảo tại nhà hàng Bãi Ông. Các món ăn đặc sản như ốc vú nàng, cua đá, rau rừng Cù Lao ăn kèm mắm nêm sẽ mang lại hương vị khó quên. Không gian nhà hàng mở hướng biển giúp quý khách vừa dùng bữa vừa tận hưởng tiếng sóng vỗ rì rào và làn gió biển thổi vào mát rượi.\"}," +
                "{\"title\":\"14:00 - Tham quan di tích đảo\",\"content\":\"Hướng dẫn viên đưa quý khách tham quan các di tích lịch sử quan trọng trên đảo như Chùa Hải Tạng cổ kính - nơi cầu may của ngư dân, Giếng cổ người Chăm với nguồn nước ngọt không bao giờ cạn. Quý khách còn được ghé thăm khu bảo tồn biển để hiểu về các loài sinh vật quý hiếm và ý thức bảo vệ môi trường biển vô giá của cụm đảo được UNESCO công nhận là khu dự trữ sinh quyển thế giới.\"}," +
                "{\"title\":\"15:30 - Về lại đất liền\",\"content\":\"Chào tạm biệt Cù Lao Chàm, tàu cao tốc đưa đoàn trở về cảng Cửa Đại. Xe sẽ chờ sẵn để đón quý khách trở về lại các điểm ban đầu tại Đà Nẵng hoặc Hội An. Kết thúc một ngày hành trình đầy ý nghĩa với muôn vàn trải nghiệm tuyệt vời giữa đại dương xanh thẳm. Chúng tôi hy vọng chuyến đi đã mang lại cho bạn những phút giây thư giãn và những bức ảnh kỷ niệm thật đẹp bên người thân.\"}" +
                "]";

            String marbleHoiAnItinerary = "[" +
                "{\"title\":\"15:30 - Ngũ Hành Sơn\",\"content\":\"Xe và hướng dẫn viên đón quý khách tại điểm hẹn, khởi hành tham quan danh thắng Ngũ Hành Sơn - cụm 5 ngọn núi đá vôi nhô lên giữa lòng thành phố. Quý khách sẽ được tham quan ngọn núi Thủy Sơn với hệ thống các hang động huyền ảo như Động Huyền Không, Động Tàng Chơn và chiêm bái các ngôi chùa cổ tự như Chùa Linh Ứng, Chùa Tam Thai. Cảm giác chinh phục các bậc đá và ngắm nhìn toàn cảnh biển Non Nước từ trên cao sẽ là khởi đầu tuyệt vời cho chuyến đi.\"}," +
                "{\"title\":\"17:30 - Phố cổ Hội An\",\"content\":\"Rời Ngũ Hành Sơn, đoàn tiếp tục di chuyển về Phố cổ Hội An. Khi trời bắt đầu chuyển sang ánh hoàng hôn, Phố Hội trở nên lung linh và thơ mộng lạ thường. Quý khách dạo bước quanh các con phố nhỏ rêu phong, tham quan Chùa Cầu biểu tượng, Nhà cổ Tân Ký - nơi lưu giữ nét kiến trúc giao thoa độc đáo. Không gian hoài cổ cùng hàng ngàn chiếc đèn lồng rực rỡ sắc màu treo khắp lối sẽ mang lại cảm giác bình yên và thư thái tuyệt đối.\"}," +
                "{\"title\":\"19:00 - Bữa tối đặc sản\",\"content\":\"Thưởng thức bữa tối nồng ấm tại nhà hàng địa phương với các món ăn đặc sản nổi tiếng của Phố Hội như Cao lầu, Mì Quảng, Bánh bao - Bánh vạc. Không gian nhà hàng ấm cúng, đậm chất Quảng Nam cùng sự phục vụ tận tình của nhân viên sẽ làm hài lòng mọi thực khách. Sau bữa tối, quý khách có thể tự do dạo phố, khám phá cuộc sống của người dân địa phương về đêm hoặc thưởng thức trà thảo mộc tại các quán trà mang phong cách cổ xưa.\"}," +
                "{\"title\":\"20:30 - Thả hoa đăng sông Hoài\",\"content\":\"Quý khách trải nghiệm đi thuyền trên dòng sông Hoài thơ mộng và tự tay thả những chiếc đèn hoa đăng lung linh lấp lánh trên mặt nước. Ánh sáng vàng dịu của nến hòa cùng bóng đèn lồng phản chiếu xuống sông tạo nên khung cảnh huyền ảo như trong truyện cổ tích. Đây là thời điểm tuyệt vời để gửi gắm những lời cầu chúc bình an cho bản thân và gia đình giữa không gian yên ả của dòng sông biểu tượng cho vùng đất nhộn nhịp một thời này.\"}," +
                "{\"title\":\"21:30 - Kết thúc hành trình\",\"content\":\"Xe đưa quý khách rời Phố cổ Hội An xinh đẹp để trở về lại thành phố Đà Nẵng. Trên đường về, quý khách có thể nghỉ ngơi trên xe và hồi tưởng lại những khoảnh khắc đẹp trong suốt hành trình tham quan Ngũ Hành Sơn hùng vĩ và Hội An lung linh sắc màu. Hướng dẫn viên chia tay đoàn tại điểm đón ban đầu, khép lại một chuyến đi đầy ắp tiếng cười và những trải nghiệm văn hóa ý nghĩa, hẹn gặp lại quý khách trong những hành trình khám phá miền Trung tiếp theo.\"}" +
                "]";

            String sontraItinerary = "[" +
                "{\"title\":\"08:00 - Khởi hành lên bán đảo\",\"content\":\"Bắt đầu hành trình chinh phục bán đảo Sơn Trà - lá phổi xanh của Đà Nẵng. Xe và hướng dẫn viên đón quý khách tại điểm hẹn, khởi hành dọc theo con đường biển tuyệt đẹp Võ Nguyên Giáp. Quý khách sẽ được tận hưởng làn gió biển mát rượi và nghe giới thiệu về những điểm dừng chân thú vị trên bán đảo xinh đẹp này.\"}," +
                "{\"title\":\"09:30 - Viếng Linh Ứng Tự\",\"content\":\"Tham quan chùa Linh Ứng Bãi Bụt, ngôi chùa lớn nhất và đẹp nhất tại Đà Nẵng. Điểm nhấn là tượng Phật Bà Quan Thế Âm cao 67m hướng mắt ra biển Đông che chở cho ngư dân. Quý khách sẽ cảm nhận được sự thanh tịnh, bình yên giữa không gian kiến trúc truyền thống hòa quyện cùng cảnh sắc thiên nhiên hùng vĩ của núi rừng và biển cả.\"}," +
                "{\"title\":\"11:00 - Khám phá đỉnh Bàn Cờ\",\"content\":\"Tiếp tục hành trình chinh phục đỉnh Bàn Cờ - nơi được mệnh danh là nóc nhà của Đà Nẵng. Quý khách sẽ được thử tài đánh cờ với tiên ông trên đỉnh núi và chiêm ngưỡng toàn cảnh thành phố cùng vịnh Đà Nẵng từ trên cao. Cảm giác chinh phục độ cao và không gian khoáng đạt nơi đây chắc chắn sẽ mang lại những bức ảnh kỷ niệm vô cùng độc đáo.\"}," +
                "{\"title\":\"14:00 - Check-in Cây Đa Nghìn Năm\",\"content\":\"Ghé thăm cây Đa nghìn năm tuổi với bộ rễ khổng lồ cắm sâu vào lòng đất tạo nên một khung cảnh kỳ ảo như trong những bộ phim thần thoại. Đây là điểm dừng chân lý tưởng để quý khách nghỉ ngơi, hít thở bầu không khí trong lành tuyệt đối của rừng nguyên sinh Sơn Trà và tìm hiểu về hệ sinh thái thực vật đa dạng quý hiếm của vùng bán đảo này.\"}," +
                "{\"title\":\"16:00 - Tạm biệt Sơn Trà\",\"content\":\"Xe bắt đầu đưa đoàn xuống núi, dọc theo những cung đường uốn lượn để trở về trung tâm thành phố. Một lần nữa quý khách được ngắm nhìn bãi biển Mỹ Khê xinh đẹp và những cây cầu nổi tiếng của Đà Nẵng từ xa. Hướng dẫn viên chia tay đoàn tại điểm đón ban đầu, khép lại một ngày khám phá thiên nhiên Sơn Trà đầy dư âm tốt đẹp và ý nghĩa.\"}" +
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
                    "Thưởng thức ẩm thực buffet quốc tế phong phú tại nhà hàng 5 sao"
                )
            );

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
                    "Tận hưởng không gian nghỉ dưỡng biển xanh cát trắng tại Bãi Ông"
                )
            );

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
                    "Tìm hiểu lịch sử văn hóa sâu sắc qua lời kể của hướng dẫn viên chuyên nghiệp"
                )
            );

            log.info("Seed: Premium tour content (descriptions & highlights & itineraries) updated.");
        } catch (Exception e) {
            log.error("Premium seeding error: {}", e.getMessage());
        }
    }

    private void updateTourWithPremiumContent(String nameLike, String description, String whyChooseUs, String itinerary, List<String> highlights) {
        // Find Tour ID
        List<Map<String, Object>> tours = jdbcTemplate.queryForList("SELECT TourID FROM Tours WHERE TourName LIKE ?", "%" + nameLike + "%");
        for (Map<String, Object> t : tours) {
            Long tourId = ((Number) t.get("TourID")).longValue();
            
            // Update Text Data
            jdbcTemplate.update("UPDATE Tours SET Description = ?, WhyChooseUs = ?, Itinerary = ? WHERE TourID = ?", description, whyChooseUs, itinerary, tourId);
            
            // Update Highlights (Delete and Re-insert)
            jdbcTemplate.update("DELETE FROM TourHighlights WHERE TourID = ?", tourId);
            for (String h : highlights) {
                jdbcTemplate.update("INSERT INTO TourHighlights (TourID, Highlight, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", tourId, h);
            }

            // --- FIX START: Xóa dữ liệu phụ thuộc để tránh lỗi Foreign Key ---
            // Xóa hình ảnh hoạt động của lịch trình
            jdbcTemplate.update("DELETE FROM TourActivityImages WHERE ScheduleID IN (SELECT ScheduleID FROM TourSchedules WHERE TourID = ?)", tourId);
            // Xóa chi tiết lịch trình (nếu có)
            jdbcTemplate.update("DELETE FROM TourScheduleDetails WHERE ScheduleID IN (SELECT ScheduleID FROM TourSchedules WHERE TourID = ?)", tourId);
            // --- FIX END ---

            // Update Schedules (Add 4 dynamic days starting from current time)
            jdbcTemplate.update("DELETE FROM TourSchedules WHERE TourID = ?", tourId);
            for (int i = 1; i <= 4; i++) {
                jdbcTemplate.update("INSERT INTO TourSchedules (TourID, StartDate, EndDate, AvailableSlots, MaxSlots, Status, CreatedAt, UpdatedAt) " +
                    "VALUES (?, DATEADD(day, ?, GETDATE()), DATEADD(day, ?, GETDATE()), 20, 20, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    tourId, i, i);
            }
        }
    }

    private void ensureDetailedItinerariesForAllTours() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList(
                "SELECT TourID, TourName, Duration, Itinerary FROM Tours"
            );

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
        if (itineraryJson == null || itineraryJson.trim().isEmpty()) return false;
        String trimmed = itineraryJson.trim();
        if (!trimmed.startsWith("[")) return false;

        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (!node.isArray() || node.size() < 4) return false;

            for (JsonNode item : node) {
                String content = item.path("content").asText("");
                if (wordCount(content) < 30) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int wordCount(String text) {
        if (text == null) return 0;
        String cleaned = text.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) return 0;
        return cleaned.split(" ").length;
    }

    private String generateDetailedItineraryJson(String tourName, Integer duration) throws Exception {
        String safeName = (tourName == null || tourName.isBlank()) ? "hành trình" : tourName.trim();
        String durationText = (duration == null || duration <= 0) ? "trong ngày" : (duration + " ngày");

        List<Map<String, String>> items = List.of(
            Map.of(
                "title", "07:30 - Đón khách & khởi hành",
                "content", "Hướng dẫn viên liên hệ trước, đón quý khách tại điểm hẹn và hỗ trợ sắp xếp chỗ ngồi. Trên xe, chúng tôi giới thiệu tổng quan về " + safeName + ", các điểm dừng chính và lưu ý an toàn. Quý khách nhận nước uống, nghe thuyết minh văn hóa địa phương và khởi động tinh thần cho trải nghiệm " + durationText + " thật trọn vẹn."
            ),
            Map.of(
                "title", "09:00 - Check-in điểm tham quan đầu tiên",
                "content", "Đoàn đến điểm tham quan nổi bật mở đầu, tự do chụp ảnh và nghe câu chuyện lịch sử gắn với địa danh. Hướng dẫn viên gợi ý góc chụp đẹp, hỗ trợ sắp xếp nhóm và nhắc nhở bảo vệ môi trường. Quý khách có thời gian thư thả cảm nhận không khí, ghi lại khoảnh khắc và bắt đầu hòa nhịp với nhịp sống bản địa."
            ),
            Map.of(
                "title", "11:30 - Trải nghiệm địa phương & nghỉ trưa",
                "content", "Chúng tôi dừng chân tại khu vực tiện nghi để nghỉ ngơi, dùng bữa và nạp năng lượng. Thực đơn ưu tiên đặc sản vùng miền, có lựa chọn nhẹ nhàng cho người lớn tuổi và trẻ em. Hướng dẫn viên chia sẻ thêm về tập quán, ẩm thực và gợi ý hoạt động phù hợp, giúp quý khách vừa thư giãn vừa hiểu sâu hơn về điểm đến."
            ),
            Map.of(
                "title", "14:00 - Khám phá điểm chính & hoạt động theo chủ đề",
                "content", "Buổi chiều là phần trải nghiệm đặc sắc nhất của " + safeName + ": tham quan theo lộ trình tối ưu, kết hợp hoạt động tương tác theo chủ đề tour. Quý khách được hướng dẫn từng bước, đảm bảo an toàn và có thời gian tự do mua sắm quà lưu niệm. Chúng tôi luôn giữ nhịp đi hợp lý để mọi người đều thoải mái, không vội vàng nhưng vẫn đủ điểm nổi bật."
            ),
            Map.of(
                "title", "17:30 - Tổng kết, đưa khách về điểm đón",
                "content", "Kết thúc lịch trình, đoàn tập trung điểm danh và lên xe trở về. Trên đường về, hướng dẫn viên tổng kết hành trình, hỗ trợ kiểm tra đồ dùng cá nhân và giải đáp thắc mắc. Quý khách nhận hình ảnh gợi ý, lời cảm ơn và hướng dẫn cho lần trải nghiệm tiếp theo. Chúng tôi đưa quý khách về đúng điểm hẹn, kết thúc chuyến đi an toàn và nhiều kỷ niệm."
            )
        );

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
                        Integer.class, tourId, i
                    );
                    if (exists != null && exists > 0) continue;

                    jdbcTemplate.update(
                        "INSERT INTO TourSchedules (TourID, StartDate, EndDate, AvailableSlots, MaxSlots, Status, CreatedAt, UpdatedAt) " +
                            "VALUES (?, DATEADD(day, ?, GETDATE()), DATEADD(day, ?, GETDATE()), 20, 20, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tourId, i, i
                    );
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
                "SELECT TourID, TourName FROM Tours t WHERE NOT EXISTS (SELECT 1 FROM TourHighlights h WHERE h.TourID = t.TourID)"
            );

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
                    "Đồng hành cùng đội ngũ hướng dẫn viên am hiểu, hỗ trợ chụp ảnh check-in tuyệt đẹp"
                );

                for (String h : highlights) {
                    jdbcTemplate.update("INSERT INTO TourHighlights (TourID, Highlight, CreatedAt, UpdatedAt) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", tourId, h);
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
            String[] adminEmails = {"admin@dana.com", "admin@gmail.com"};
            // BCrypt hash cho "123456"
            String passwordHash = "$2a$10$7vj26Aptw/yE0uT/8f6BGe.1e.W0U9WfNn0/2fV9rUfB5W1N8yD9w";
            
            for (String email : adminEmails) {
                List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT UserID FROM Users WHERE Email = ?", email);
                
                if (users.isEmpty()) {
                    jdbcTemplate.update(
                        "INSERT INTO Users (Email, FullName, PasswordHash, Role, IsActive, CreatedAt, UpdatedAt) VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        email, "Admin Account", passwordHash, "ADMIN"
                    );
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
}
