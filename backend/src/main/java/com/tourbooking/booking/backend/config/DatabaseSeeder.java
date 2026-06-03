package com.tourbooking.booking.backend.config;

import com.tourbooking.booking.backend.model.entity.Category;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.TourStatus;
import com.tourbooking.booking.backend.repository.CategoryRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
// @Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

        private final CategoryRepository categoryRepository;
        private final TourRepository tourRepository;
        private final DiscountRepository discountRepository;

        private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        @Override
        public void run(String... args) throws Exception {
                log.info("Bắt đầu cập nhật dữ liệu mẫu...");
                
                // Vẫn kiểm tra seeding cơ bản nhưng tối giản nhất
                if (discountRepository.count() == 0)
                        seedDiscounts();
                if (categoryRepository.count() == 0)
                        seedCategories();
                // Chỉ tạo tour mẫu nếu DB hoàn toàn trống (giúp khởi động siêu nhanh)
                if (tourRepository.count() == 0)
                        seedTours();

                log.info("✅ Dữ liệu đã sẵn sàng, server khởi động hoàn tất!");

                try {
                        // Cập nhật tất cả tour về giá thực tế hoặc mặc định (tùy chọn)
                        // jdbcTemplate.execute("UPDATE Tours SET Price = 10000"); 
                } catch (Exception e) {
                        log.error("Lỗi khi cập nhật giá tour: " + e.getMessage());
                }
        }

        private void seedDiscounts() {
                // SUMMER 20%
                com.tourbooking.booking.backend.model.entity.Discount d1 = new com.tourbooking.booking.backend.model.entity.Discount();
                d1.setCode("SUMMER");
                d1.setDiscountType(com.tourbooking.booking.backend.model.entity.enums.DiscountType.PERCENTAGE);
                d1.setValue(new BigDecimal("20"));
                d1.setIsActive(true);
                d1.setStartDate(LocalDate.now().minusDays(10).atStartOfDay());
                d1.setEndDate(LocalDate.now().plusMonths(3).atStartOfDay());
                d1.setUsageLimit(1000);
                discountRepository.save(d1);

                // WELCOME 10%
                com.tourbooking.booking.backend.model.entity.Discount d2 = new com.tourbooking.booking.backend.model.entity.Discount();
                d2.setCode("WELCOME10");
                d2.setDiscountType(com.tourbooking.booking.backend.model.entity.enums.DiscountType.PERCENTAGE);
                d2.setValue(new BigDecimal("10"));
                d2.setIsActive(true);
                d2.setStartDate(LocalDate.now().minusDays(30).atStartOfDay());
                d2.setEndDate(LocalDate.now().plusYears(1).atStartOfDay());
                discountRepository.save(d2);

                // HOLIDAY 500k
                com.tourbooking.booking.backend.model.entity.Discount d3 = new com.tourbooking.booking.backend.model.entity.Discount();
                d3.setCode("HOLIDAY");
                d3.setDiscountType(com.tourbooking.booking.backend.model.entity.enums.DiscountType.FIXED_AMOUNT);
                d3.setValue(new BigDecimal("500000"));
                d3.setIsActive(true);
                d3.setMinimumBookingAmount(new BigDecimal("2000000"));
                discountRepository.save(d3);
        }

        private void seedCategories() {
                List<String> categories = Arrays.asList(
                                "Tour Bà Nà Hills", "Tour Hội An", "Tour Biển Đảo",
                                "Tour Văn Hóa", "Tour Mạo Hiểm", "Tour Ẩm Thực",
                                "Tour Nghỉ Dưỡng", "Tour Tâm Linh");
                categories.forEach(name -> {
                        Category cat = new Category();
                        cat.setCategoryName(name);
                        cat.setDescription("Hành trình khám phá " + name + " cùng Dana.");
                        categoryRepository.save(cat);
                });
        }

        private void seedTours() {
                List<Category> allCats = categoryRepository.findAll();
                if (allCats.size() < 8)
                        return;

                // 1. TOUR BÀ NÀ HILLS
                createTour("Bà Nà Hills - Đường lên tiên cảnh & Cầu Vàng",
                                "Hành trình khám phá Bà Nà Hills sẽ đưa quý khách đến với một trong những điểm du lịch hấp dẫn nhất Việt Nam. Nằm ở độ cao 1.487m so với mực nước biển, Bà Nà Hills được mệnh danh là 'Đà Lạt của miền Trung' với khí hậu bốn mùa trong một ngày. Điểm nhấn không thể bỏ qua là Cầu Vàng (Golden Bridge) - một kiệt tác kiến trúc được nâng đỡ bởi đôi bàn tay khổng lồ rêu phong, vươn ra giữa lưng chừng mây trời. Quý khách sẽ được trải nghiệm tuyến cáp treo đạt nhiều kỷ lục thế giới, ngắm nhìn toàn cảnh rừng nguyên sinh hùng vĩ và thác Tóc Tiên thơ mộng từ trên cao. Tại làng Pháp, không gian kiến trúc Gothic cổ điển với những lâu đài rêu phong sẽ đưa bạn lạc vào châu Âu thế kỷ XIX. Ngoài ra, khu vui chơi giải trí Fantasy Park mang đến những trải nghiệm mạo hiểm và sôi động cho mọi lứa tuổi. Chuyến đi không chỉ là tham quan mà còn là sự tận hưởng dịch vụ cao cấp, ẩm thực buffet đa dạng và những màn trình diễn nghệ thuật đường phố đặc sắc. Hãy cùng Dana tạo nên những kỷ niệm khó quên tại chốn bồng lai tiên cảnh này!",
                                "[{\"title\":\"08:00 - Khởi hành\",\"content\":\"Xe đón tại khách sạn.\"},{\"title\":\"09:30 - Cầu Vàng\",\"content\":\"Tham quan Cầu Vàng, Vườn hoa, Hầm rượu.\"},{\"title\":\"12:00 - Buffet Trưa\",\"content\":\"Bữa trưa tại nhà hàng 5 sao.\"},{\"title\":\"14:00 - Fantasy Park\",\"content\":\"Khu vui chơi giải trí trong nhà.\"},{\"title\":\"16:30 - Trở về\",\"content\":\"Xuống cáp treo và về Đà Nẵng.\"}]",
                                10, 1, "Đà Nẵng", allCats.get(0), 5.0, "Xe du lịch",
                                "Mọi lứa tuổi, đặc biệt là Gia đình & Cặp đôi",
                                "Miễn phí cho trẻ em dưới 1m. Trẻ em từ 1m - 1m4 tính giá trẻ em.");

                // 2. TOUR HỘI AN
                createTour("Hội An Phố Cổ - Lung linh sắc màu di sản",
                                "Hội An - nơi thời gian dường như ngưng đọng trên những bức tường vàng cổ kính và những nếp nhà ngói rêu phong. Hành trình trải nghiệm Phố Cổ cùng Dana sẽ đưa bạn đi sâu vào từng ngõ nhỏ, khám phá những câu chuyện văn hóa lịch sử hàng trăm năm. Quý khách sẽ được chiêm ngưỡng Chùa Cầu - biểu tượng của sự giao thoa kiến trúc Việt - Nhật, tham quan các hội quán Phúc Kiến, Quảng Đông và những ngôi nhà cổ lâu đời như Tân Ký hay Phùng Hưng. Về đêm, Hội An khoác lên mình vẻ đẹp huyền ảo với hàng ngàn ánh đèn lồng đủ màu sắc soi bóng bên sông Hoài thơ mộng. Bạn sẽ có cơ hội ngồi thuyền thả đèn hoa đăng cầu may mắn, thưởng thức các món ăn đặc sản đậm chất địa phương như Cao Lầu, cơm gà hay bánh mì Phượng. Đặc biệt, chúng tôi sẽ đưa bạn tham quan các làng nghề truyền thống như làng gốm Thanh Hà hay đảo Cẩm Nam. Đây là tour du lịch văn hóa đích thực, dành cho những ai yêu thích sự yên bình và muốn tìm hiểu về hồn cốt dân tộc trong từng nhịp sống chậm rãi của phố hội.",
                                "[{\"title\":\"15:30 - Khởi hành\",\"content\":\"Xe đón đi phố cổ Hội An.\"},{\"title\":\"16:30 - Tham quan\",\"content\":\"Chùa Cầu, Nhà cổ, Hội quán.\"},{\"title\":\"18:30 - Ăn tối\",\"content\":\"Đặc sản Cao Lầu, Cơm gà.\"},{\"title\":\"20:00 - Sông Hoài\",\"content\":\"Thả đèn hoa đăng trên sông.\"},{\"title\":\"21:00 - Trở về\",\"content\":\"Kết thúc tour tại Đà Nẵng.\"}]",
                                20, 1, "Hội An", allCats.get(1), 4.8, "Xe điện",
                                "Khách yêu thích văn hóa, người lớn tuổi & nhiánh",
                                "Phù hợp cho mọi trẻ em. Giảm 50% cho trẻ từ 5-10 tuổi.");

                // 3. TOUR CÙ LAO CHÀM
                createTour("Cù Lao Chàm - Thiên đường biển xanh vẫy gọi",
                                "Cù Lao Chàm là một cụm đảo xinh đẹp được UNESCO công nhận là Khu dự trữ sinh quyển thế giới. Tour Cù Lao Chàm của chúng tôi sẽ đưa bạn rời xa sự ồn ào phố thị để hòa mình vào làn nước trong xanh nhìn thấy tận đáy. Quý khách sẽ được trải nghiệm cảm giác lướt sóng bằng cano cao tốc cực kỳ phấn khích. Tại đây, bạn sẽ được tham quan Chùa Hải Tạng cổ kính, giếng cổ Chăm Pa và khu bảo tồn biển. Hoạt động lặn ngắm san hô (snorkeling) sẽ là điểm nhấn khó quên khi bạn được chiêm ngưỡng những rặng san hô đa sắc màu và các loài sinh vật biển phong phú. Bữa trưa với các món hải sản tươi sống và rau rừng đặc hữu sẽ làm hài lòng những thực khách khó tính nhất. Sau đó, quý khách có thể tự do tắm biển tại Bãi Chồng hoặc Bãi Ông, hay nằm thư giãn trên những chiếc võng dưới hàng dừa xanh mát. Đây là sự lựa chọn hoàn hảo cho những ai yêu thiên nhiên hoang sơ và muốn tìm kiếm một ngày nghỉ ngơi thực thụ bên bờ biển miền Trung tuyệt đẹp.",
                                "[{\"title\":\"08:00 - Cửa Đại\",\"content\":\"Đón khách tại bến tàu Cửa Đại.\"},{\"title\":\"09:00 - Khám phá đảo\",\"content\":\"Tham quan chùa, giếng cổ, chợ quê.\"},{\"title\":\"10:30 - Lặn san hô\",\"content\":\"Trải nghiệm lặn ngắm san hô tại hòn Dài.\"},{\"title\":\"12:00 - Hải sản\",\"content\":\"Ăn trưa hải sản tươi tại Bãi Chồng.\"},{\"title\":\"15:00 - Về đất liền\",\"content\":\"Cano đưa khách về lại bến.\"}]",
                                30, 1, "Hội An", allCats.get(2), 4.7, "Cano cao tốc",
                                "Thanh niên, người yêu biển, không phù hợp người bị say sóng nặng",
                                "Trẻ em dưới 3 tuổi tính phí cano 100k. Không khuyến khích trẻ nhỏ lặn san hô vùng nước sâu.");

                // 4. TOUR CỐ ĐÔ HUẾ
                createTour("Cố Đô Huế - Vẻ đẹp trầm mặc của kinh thành xưa",
                                "Hành trình từ Đà Nẵng đi Cố đô Huế sẽ đưa quý khách xuyên qua hầm đường bộ Hải Vân hùng vĩ để đến với vùng đất của những di sản. Huế nổi tiếng với vẻ đẹp dịu dàng, thơ mộng bên dòng sông Hương hiền hòa. Trong tour này, quý khách sẽ được tham quan Đại Nội - hoàng cung của 13 vị vua triều Nguyễn với những kiến trúc nghệ thuật cung đình đặc sắc như Ngọ Môn, Điện Thái Hòa, Thế Miếu. Chúng tôi cũng sẽ đưa bạn đến chiêm bái Chùa Thiên Mụ - ngôi chùa cổ nhất và là biểu tượng của mảnh đất thần kinh. Tiếp theo là hành trình khám phá các lăng tẩm uy nghi như Lăng Khải Định với sự giao thoa kiến trúc Đông Tây độc đáo. Bữa trưa với những món ăn cung đình hay đặc sản bún bò Huế chắc chắn sẽ để lại ấn tượng khó phai. Huế không chỉ là lịch sử, mà còn là văn hóa, con người và nhịp sống chậm rãi rất riêng. Đây là tour du lịch không thể bỏ qua đối với những người yêu thích tìm hiểu về cội nguồn và giá trị truyền thống của dân tộc Việt Nam.",
                                "[{\"title\":\"07:30 - Hải Vân\",\"content\":\"Khởi hành đi Huế qua hầm Hải Vân.\"},{\"title\":\"10:00 - Đại Nội\",\"content\":\"Tham quan kinh thành Huế xưa.\"},{\"title\":\"12:30 - Ẩm thực Huế\",\"content\":\"Ăn trưa đặc sản cố đô.\"},{\"title\":\"14:00 - Lăng Khải Định\",\"content\":\"Tham quan kiến trúc lăng tẩm.\"},{\"title\":\"16:30 - Trở về\",\"content\":\"Xe đưa khách về Đà Nẵng.\"}]",
                                40, 1, "Huế", allCats.get(3), 4.9, "Xe du lịch",
                                "Yêu thích lịch sử, gia đình có người lớn tuổi",
                                "Dặm đường xa, khuyến khích trẻ em có sức khỏe tốt.");

                // 5. TOUR NGŨ HÀNH SƠN - SƠN TRÀ
                createTour("Ngũ Hành Sơn & Bán đảo Sơn Trà - Linh ứng tâm linh",
                                "Khám phá sự kết hợp hoàn hảo giữa vẻ đẹp núi non kỳ vĩ và tâm linh sâu sắc tại Đà Nẵng. Điểm dừng chân đầu tiên là Bán đảo Sơn Trà, nơi có Chùa Linh Ứng Bãi Bụt với tượng Phật Bà Quan Âm cao nhất Việt Nam, hướng nhìn ra biển Đông bao la. Quý khách sẽ cảm nhận được sự thanh tịnh và không khí trong lành giữa núi rừng nguyên sinh. Tiếp đến là khu danh thắng Ngũ Hành Sơn - tuyệt tác của thiên nhiên với 5 ngọn núi Kim - Mộc - Thủy - Hỏa - Thổ. Tại đây, bạn sẽ được khám phá những hang động huyền bí như động Huyền Không, động Tàng Chơn, chiêm bái các ngôi chùa cổ tự và ngắm nhìn toàn cảnh thành phố từ vọng giang đài. Chuyến đi còn đưa bạn ghé thăm Làng đá mỹ nghệ Non Nước lâu đời, nơi các nghệ nhân tạo ra những tác phẩm tinh xảo từ đá tự nhiên. Đây là hành trình mang lại sự cân bằng giữa trải nghiệm tham quan và tìm về sự bình an trong tâm hồn.",
                                "[{\"title\":\"14:00 - Sơn Trà\",\"content\":\"Tham quan chùa Linh Ứng Bãi Bụt.\"},{\"title\":\"15:30 - Ngũ Hành Sơn\",\"content\":\"Tham quan hệ thống hang động và chùa.\"},{\"title\":\"17:30 - Làng đá\",\"content\":\"Xem nghệ thuật tạc đá Non Nước.\"},{\"title\":\"19:00 - Ăn tối\",\"content\":\"Thưởng thức bánh tráng thịt heo.\"},{\"title\":\"21:00 - Kết thúc\",\"content\":\"Về lại điểm đón ban đầu.\"}]",
                                50, 1, "Đà Nẵng", allCats.get(7), 4.7, "Xe du lịch",
                                "Khách hành hương, yêu thiên nhiên",
                                "Leo núi nhiều, cần chú ý quan sát trẻ nhỏ.");

                // 6. TOUR THÁNH ĐỊA MỸ SƠN
                createTour("Thánh địa Mỹ Sơn - Dấu ấn vương triều Chăm Pa",
                                "Thánh địa Mỹ Sơn là di sản văn hóa thế giới được mệnh danh là 'Thung lũng của những vị thần'. Nằm trong một thung lũng hẹp bao quanh bởi núi non hùng vĩ, đây từng là trung tâm cúng tế và là khu vực lăng mộ của các vị vua Chăm Pa cổ đại. Quý khách sẽ được tận mắt chứng kiến những tòa tháp gạch đỏ rêu phong với kỹ thuật xây dựng bí ẩn đến nay vẫn làm đau đầu các nhà khoa học. Những nét chạm khắc tinh xảo về các vị thần Shiva, vũ nữ Apsara mang đậm phong cách Ấn Độ giáo sẽ dẫn dắt bạn trở về với quá khứ hào hùng của một vương triều lẫy lừng. Đặc biệt, bạn sẽ được thưởng thức điệu múa Chăm quyến rũ ngay giữa không gian cổ kính của các đền đài. Hành trình này lý tưởng cho những du khách đam mê khảo cổ, nghệ thuật điêu khắc và muốn tìm hiểu sâu về sự đa dạng văn hóa của dải đất miền Trung Việt Nam.",
                                "[{\"title\":\"08:00 - Khởi hành\",\"content\":\"Lên đường đi Mỹ Sơn.\"},{\"title\":\"09:30 - Tham quan tháp\",\"content\":\"Khám phá các tổ hợp đền tháp cổ.\"},{\"title\":\"10:30 - Xem múa Chăm\",\"content\":\"Thưởng thức nghệ thuật Apsara.\"},{\"title\":\"12:00 - Ăn trưa\",\"content\":\"Bữa trưa món ăn địa phương.\"},{\"title\":\"14:00 - Kết thúc\",\"content\":\"Xe đưa khách về lại điểm hẹn.\"}]",
                                60, 1, "Quảng Nam", allCats.get(3), 4.6, "Xe du lịch",
                                "Khách quốc tế, sinh viên & người mê khảo cổ",
                                "Phù hợp mọi lứa tuổi.");

                // 7. TOUR ẨM THỰC ĐÀ NẴNG
                createTour("Đà Nẵng Food Tour - Thiên đường ẩm thực về đêm",
                                "Đà Nẵng không chỉ đẹp bởi cảnh sắc mà còn quyến rũ bởi nền ẩm thực phong phú và đặc sắc. Food Tour của chúng tôi sẽ đưa bạn 'ăn cả thế giới' tại thành phố biển này. Quý khách sẽ được hướng dẫn viên sành ăn dẫn đi khám phá những quán ăn gia truyền nằm sâu trong những con ngõ nhỏ mà chỉ người dân địa phương mới biết. Bắt đầu từ món bánh xèo, nem lụi giòn tan, tiếp đến là tô Mỳ Quảng đậm đà hay bánh tráng cuốn thịt heo hai đầu da độc đáo. Không thể bỏ qua những món ăn vặt nức tiếng như ốc hút, bánh tráng kẹp hay chè liên. Hành trình còn kết hợp tham quan các khu chợ đêm sầm uất, nơi bạn có thể cảm nhận nhịp sống sôi động về khuya của người dân Đà Nẵng. Đây không đơn thuần là một tour ăn uống, mà còn là sự giao lưu văn hóa và cảm nhận tình người Đà Thành hiếu khách thông qua hương vị của những món ăn tâm huyết.",
                                "[{\"title\":\"17:30 - Ăn nhẹ\",\"content\":\"Bánh bèo, lọc, nậm tại chợ.\"},{\"title\":\"18:30 - Món chính\",\"content\":\"Mỳ Quảng hoặc Bánh Xèo.\"},{\"title\":\"20:00 - Phố ăn vặt\",\"content\":\"Ốc hút và đặc sản đường phố.\"},{\"title\":\"21:00 - Chè Liên\",\"content\":\"Giải nhiệt với chè sầu riêng.\"},{\"title\":\"22:00 - Kết thúc\",\"content\":\"Tự do dạo biển đêm.\"}]",
                                70, 1, "Đà Nẵng", allCats.get(5), 4.9, "Xe máy / Đi bộ",
                                "Thanh niên, khách sành ăn (Foodies)",
                                "Chế độ ăn nhiều dầu mỡ/cay, cha mẹ lưu ý thực đơn cho bé.");

                // 8. TOUR RỪNG DỪA BẢY MẪU
                createTour("Rừng Dừa Bảy Mẫu - Trải nghiệm miền Tây giữa miền Trung",
                                "Rừng dừa Bảy Mẫu là một điểm đến sinh thái xanh mát nằm ngay gần Hội An. Tại đây, quý khách sẽ được trải nghiệm ngồi trên những chiếc thúng chai bập bềnh len lỏi qua những rạch dừa nước xanh mướt, gợi nhớ về không gian của miền Tây Nam Bộ. Điều thú vị nhất chính là màn biểu diễn 'quay thúng' điêu luyện của các nghệ nhân địa phương, mang lại những tràng cười sảng khoái và cảm giác mạnh thú vị. Ngoài ra, bạn còn có thể tham gia các trò chơi dân gian như câu cua, đua thúng, hay trổ tài đan các vật dụng xinh xắn từ lá dừa. Sau những giờ phút vui chơi, bữa trưa dân dã với các món ăn miền sông nước tại nhà sàn sẽ mang lại cảm giác bình yên đến lạ. Đây là tour du lịch sinh thái đặc sắc, rất phù hợp cho các nhóm khách gia đình và những người muốn có những trải nghiệm gần gũi nhất với thiên nhiên và nếp sống mộc mạc của người nông dân miền Trung.",
                                "[{\"title\":\"08:30 - Cẩm Thanh\",\"content\":\"Đến khu sinh thái rừng dừa.\"},{\"title\":\"09:30 - Chèo thúng\",\"content\":\"Tham quan rạch dừa nước.\"},{\"title\":\"10:30 - Quay thúng\",\"content\":\"Xem biểu diễn quay thúng chai.\"},{\"title\":\"12:00 - Cơm quê\",\"content\":\"Ăn trưa đặc sản vườn dừa.\"},{\"title\":\"14:00 - Kết thúc\",\"content\":\"Rời rừng dừa về lại điểm đón.\"}]",
                                80, 1, "Hội An", allCats.get(1), 4.8, "Thúng / Xe điện",
                                "Gia đình có trẻ nhỏ, khách đoàn teambuilding",
                                "Rất thú vị cho trẻ em. Lưu ý mặc áo phao cho bé khi ngồi thúng.");
        }

        private void createTour(String name, String desc, String itinerary, int price, int duration, String startLoc,
                        Category cat, double rating, String trans, String suitable, String policy) {
                Tour t = new Tour();
                t.setTourName(name);
                t.setDescription(desc);
                t.setItinerary(itinerary);
                t.setPrice(new BigDecimal(price));
                t.setDuration(duration);
                t.setStartLocation(startLoc);
                t.setCategory(cat);
                t.setRating(rating);
                t.setTransportType(trans);
                t.setSuitableAges(suitable);
                t.setChildPolicy(policy);
                t.setWhyChooseUs(
                                "✅ Giá tốt nhất thị trường\n✅ Hướng dẫn viên chuyên nghiệp\n✅ Cam kết chất lượng dịch vụ 5 sao");

                List<TourSchedule> schedules = new ArrayList<>();
                // Tạo 15 lịch trình cho mỗi tour để đạt mốc ~100+ schedules
                for (int i = 0; i < 15; i++) {
                        TourSchedule s = new TourSchedule();
                        s.setStartDate(LocalDate.now().plusDays(i * 3 + (int) (Math.random() * 3)));
                        s.setEndDate(s.getStartDate().plusDays(duration - 1));
                        s.setAvailableSlots(20 + (int) (Math.random() * 10));
                        s.setStatus(TourStatus.OPEN);
                        s.setTour(t);
                        schedules.add(s);
                }
                t.setSchedules(schedules);
                tourRepository.save(t);
        }
}
