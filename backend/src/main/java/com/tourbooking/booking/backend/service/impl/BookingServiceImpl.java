package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.mapper.BookingMapper;
import com.tourbooking.booking.backend.model.dto.request.BookingRequest;
import com.tourbooking.booking.backend.model.dto.response.BookingResponse;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.Payment;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.PaymentRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import com.tourbooking.booking.backend.repository.DiscountRepository;
import com.tourbooking.booking.backend.repository.PassengerRepository;
import com.tourbooking.booking.backend.model.entity.Discount;
import com.tourbooking.booking.backend.model.entity.Passenger;
import com.tourbooking.booking.backend.model.entity.enums.DiscountType;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;
import com.tourbooking.booking.backend.model.dto.request.PassengerRequest;
import com.tourbooking.booking.backend.repository.UserRepository;
import java.time.LocalDateTime;
import com.tourbooking.booking.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tourbooking.booking.backend.model.dto.request.VoucherRequest;
import com.tourbooking.booking.backend.model.dto.response.VoucherResponse;
import com.tourbooking.booking.backend.model.dto.request.RefundRequest;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.tourbooking.booking.backend.model.dto.response.FinancialReportResponse;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final DiscountRepository discountRepository;
    private final PaymentRepository paymentRepository;
    private final PassengerRepository passengerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TourSchedule schedule = tourScheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        int adultCount = request.getAdultCount() != null ? request.getAdultCount() : 1;
        int childCount = request.getChildCount() != null ? request.getChildCount() : 0;
        int totalPeople = adultCount + childCount;

        // ── VALIDATION 1: số phần tử passengers phải khớp với adultCount + childCount ─────────
        List<PassengerRequest> passengerList = request.getPassengers();
        if (passengerList == null || passengerList.size() != totalPeople) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Số hành khách gửi lên (" + (passengerList == null ? 0 : passengerList.size()) +
                    ") không khớp với tổng số người đặt (" + totalPeople + ").");
        }

        // ── VALIDATION 2: kiểm tra chéo tuổi thực tế — tự điều chỉnh CHILD→ADULT nếu tuổi > 11 ─
        LocalDate today = LocalDate.now();
        int effectiveAdults   = 0;
        int effectiveChildren = 0;
        List<String> resolvedTypes = new java.util.ArrayList<>();

        for (int i = 0; i < passengerList.size(); i++) {
            PassengerRequest pr = passengerList.get(i);
            String requested = pr.getPassengerType() != null
                    ? pr.getPassengerType().toUpperCase() : "ADULT";
            String resolved  = requested;

            if (pr.getDateOfBirth() != null) {
                int age = Period.between(pr.getDateOfBirth(), today).getYears();

                if ("CHILD".equals(requested) && age > 11) {
                    // Khai là trẻ em nhưng tuổi thực tế đã lớn hơn 11 → tự sửa thành ADULT
                    resolved = "ADULT";
                    log.warn("[Booking] Hành khách #{} '{}' (tuổi {}) khai CHILD nhưng được tự sửa sang ADULT.",
                            i + 1, pr.getFullName(), age);
                }
                // Nếu khai ADULT nhưng tuổi ≤ 11 → giữ nguyên ADULT (không giảm giá, bảo vệ doanh thu)
            }

            resolvedTypes.add(resolved);
            if ("CHILD".equals(resolved)) effectiveChildren++;
            else                            effectiveAdults++;
        }

        // UC13: Kiểm tra số chỗ trống
        if (schedule.getAvailableSlots() < totalPeople) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        // UC14: Tính giá dựa trên loại hành khách thực tế (sau khi đã tự sửa)
        var price      = schedule.getTour().getPrice();
        var childPrice = price.multiply(new BigDecimal("0.75"));
        var totalPrice = price.multiply(BigDecimal.valueOf(effectiveAdults))
                .add(childPrice.multiply(BigDecimal.valueOf(effectiveChildren)));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setNumberOfPeople(totalPeople);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingDate(java.time.LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // Trừ slot
        schedule.setAvailableSlots(schedule.getAvailableSlots() - totalPeople);
        tourScheduleRepository.save(schedule);

        // Lưu danh sách hành khách (dùng resolvedTypes đã chỉnh sửa)
        for (int i = 0; i < passengerList.size(); i++) {
            PassengerRequest pr = passengerList.get(i);
            Passenger p = new Passenger();
            p.setBooking(saved);
            p.setFullName(pr.getFullName());
            p.setDateOfBirth(pr.getDateOfBirth());
            p.setIdNumber(pr.getIdNumber());
            p.setPassengerType(resolvedTypes.get(i));  // dùng type đã xác thực
            passengerRepository.save(p);
        }

        // Handle Discount
        if (request.getDiscountCode() != null && !request.getDiscountCode().isEmpty()) {
            String code = request.getDiscountCode().toUpperCase();
            BigDecimal discountAmt = BigDecimal.ZERO;
            boolean applied = false;

            if ("SUMMER".equals(code)) {
                discountAmt = saved.getTotalPrice().multiply(new BigDecimal("20")).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
                saved.setDiscountCode("SUMMER");
                applied = true;
            } else {
                Discount discount = discountRepository.findByCode(code).orElse(null);
                if (discount != null && discount.getIsActive() &&
                    (discount.getStartDate() == null || !LocalDateTime.now().isBefore(discount.getStartDate())) &&
                    (discount.getEndDate() == null || !LocalDateTime.now().isAfter(discount.getEndDate())) &&
                    (discount.getUsageLimit() == null || discount.getCurrentUsage() < discount.getUsageLimit())) {

                    if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                        discountAmt = saved.getTotalPrice().multiply(discount.getValue()).divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
                    } else {
                        discountAmt = discount.getValue();
                    }
                    saved.setDiscountCode(discount.getCode());
                    applied = true;
                    // Logic tăng currentUsage được chuyển sang giai đoạn CONFIRMED
                }
            }

            if (applied) {
                saved.setDiscountAmount(discountAmt);
                saved.setTotalPrice(saved.getTotalPrice().subtract(discountAmt));
            }
        }

        Booking savedBooking = bookingRepository.save(saved);
        return BookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking existingBooking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (existingBooking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (request.getUserId() != null && !request.getUserId().equals(existingBooking.getUser().getId())) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            existingBooking.setUser(user);
        }

        if (request.getScheduleId() != null &&
                !request.getScheduleId().equals(existingBooking.getSchedule().getId())) {

            TourSchedule oldSchedule = tourScheduleRepository.findByIdWithLock(existingBooking.getSchedule().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

            TourSchedule newSchedule = tourScheduleRepository.findByIdWithLock(request.getScheduleId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

            int people = existingBooking.getNumberOfPeople();

            // check slot schedule mới
            if (newSchedule.getAvailableSlots() < people) {
                throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
            }

            // trả slot schedule cũ
            oldSchedule.setAvailableSlots(oldSchedule.getAvailableSlots() + people);

            // trừ slot schedule mới
            newSchedule.setAvailableSlots(newSchedule.getAvailableSlots() - people);

            tourScheduleRepository.save(oldSchedule);
            tourScheduleRepository.save(newSchedule);

            existingBooking.setSchedule(newSchedule);
        }
        // handle change numberOfPeople (driven by adultCount / childCount)
        if (request.getAdultCount() != null) {

            int oldValue = existingBooking.getNumberOfPeople();
            int newValue = request.getNumberOfPeople();

            int diff = newValue - oldValue;

            TourSchedule schedule = tourScheduleRepository.findByIdWithLock(existingBooking.getSchedule().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

            // nếu tăng người
            if (diff > 0 && schedule.getAvailableSlots() < diff) {
                throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
            }

            // update slot
            schedule.setAvailableSlots(schedule.getAvailableSlots() - diff);
            tourScheduleRepository.save(schedule);
        }
        BookingMapper.updateEntityFromRequest(existingBooking, request);

        Booking updatedBooking = bookingRepository.save(existingBooking);
        return BookingMapper.toResponse(updatedBooking);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }
        bookingRepository.deleteById(id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<FinancialReportResponse> getFinancialReport(String start, String end, String type, String status) {
        LocalDateTime startDateTime = LocalDate.parse(start).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(end).atTime(23, 59, 59);

        log.info("Generating financial report from {} to {}, type: {}, status: {}", start, end, type, status);

        // Lấy tất cả payments trong khoảng thời gian
        List<Payment> allPayments = paymentRepository.findAll();
        log.info("Total payments in DB: {}", allPayments.size());

        // Lọc payments theo ngày thanh toán (paymentDate hoặc createdAt)
        List<Payment> filteredPayments = allPayments.stream()
                .filter(p -> {
                    LocalDateTime pDate = p.getPaymentDate() != null ? p.getPaymentDate() : p.getCreatedAt();
                    if (pDate == null) return false;
                    return (pDate.isEqual(startDateTime) || pDate.isAfter(startDateTime)) &&
                           (pDate.isEqual(endDateTime) || pDate.isBefore(endDateTime));
                })
                .filter(p -> {
                    // Nếu filter status là "all" hoặc không có, lấy tất cả payment SUCCESS
                    if (status == null || status.isEmpty() || "all".equalsIgnoreCase(status)) {
                        return p.getStatus() == PaymentStatus.SUCCESS && !"TEST_DATA".equals(p.getPaymentMethod());
                    }
                    // Nếu filter là SUCCESS hoặc COMPLETED → lấy payment SUCCESS
                    if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                        return p.getStatus() == PaymentStatus.SUCCESS && !"TEST_DATA".equals(p.getPaymentMethod());
                    }
                    // Các trường hợp khác
                    return false;
                })
                .toList();

        log.info("Filtered payments (SUCCESS) count: {}", filteredPayments.size());

        // Lấy bookings bị CANCELLED trong khoảng thời gian (để tính cancellation rate)
        List<Booking> cancelledBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .filter(b -> {
                    LocalDateTime bDate = b.getCreatedAt() != null ? b.getCreatedAt() : b.getUpdatedAt();
                    if (bDate == null) return false;
                    return (bDate.isEqual(startDateTime) || bDate.isAfter(startDateTime)) &&
                           (bDate.isEqual(endDateTime) || bDate.isBefore(endDateTime));
                })
                .toList();

        // Formatter theo loại báo cáo
        DateTimeFormatter formatter = switch (type.toLowerCase()) {
            case "weekly"  -> DateTimeFormatter.ofPattern("yyyy-'W'ww", Locale.getDefault());
            case "monthly" -> DateTimeFormatter.ofPattern("yyyy-MM");
            case "yearly"  -> DateTimeFormatter.ofPattern("yyyy");
            default        -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // Group payments theo period
        Map<String, List<Payment>> groupedPayments = filteredPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            LocalDateTime date = p.getPaymentDate() != null ? p.getPaymentDate() : p.getCreatedAt();
                            return date != null ? date.format(formatter) : "Unknown";
                        },
                        TreeMap::new,
                        Collectors.toList()
                ));

        // Group cancellations theo period
        Map<String, Long> cancelledByPeriod = cancelledBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> {
                            LocalDateTime date = b.getCreatedAt() != null ? b.getCreatedAt() : b.getUpdatedAt();
                            return date != null ? date.format(formatter) : "Unknown";
                        },
                        Collectors.counting()
                ));

        // Nếu không có payment nào, trả về danh sách rỗng với log warning
        if (groupedPayments.isEmpty()) {
            log.warn("No SUCCESS payments found in range {} - {}", start, end);
        }

        return groupedPayments.entrySet().stream()
                .map(entry -> {
                    String period = entry.getKey();
                    List<Payment> periodPayments = entry.getValue();

                    long bookingCount = periodPayments.size();

                    // Revenue = tổng Payment.amount của các payment SUCCESS
                    BigDecimal revenue = periodPayments.stream()
                            .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long cancellations = cancelledByPeriod.getOrDefault(period, 0L);

                    BigDecimal averageValue = bookingCount > 0
                            ? revenue.divide(BigDecimal.valueOf(bookingCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    log.info("Period: {}, bookings: {}, revenue: {}, cancellations: {}", period, bookingCount, revenue, cancellations);

                    return FinancialReportResponse.builder()
                            .period(period)
                            .bookingCount(bookingCount)
                            .revenue(revenue)
                            .averageValue(averageValue)
                            .cancellations(cancellations)
                            .build();
                })
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))
                .toList();
    }

    @Override
    public long countActiveBookings() {
        long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        return confirmed + pending;
    }

    @Override
    public BigDecimal getMonthlyRevenue() {
        YearMonth current = YearMonth.now();
        LocalDateTime start = current.atDay(1).atStartOfDay();
        LocalDateTime end = current.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal revenue = paymentRepository.sumAmountByStatusAndDateBetween(
                com.tourbooking.booking.backend.model.entity.enums.PaymentStatus.SUCCESS, start, end);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public VoucherResponse applyVoucher(VoucherRequest request) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        String message = "Mã giảm giá không hợp lệ hoặc đã hết hạn";
        boolean isValid = false;
        
        // Check for specific hardcoded voucher first (legacy)
        if ("SUMMER".equalsIgnoreCase(request.getVoucherCode())) {
            discountAmount = request.getCurrentTotal().multiply(new BigDecimal("20")).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            isValid = true;
            message = "Áp dụng mã SUMMER thành công (-20%)";
        } else if ("SUMMER2026".equalsIgnoreCase(request.getVoucherCode())) {
            discountAmount = new BigDecimal("500000");
            isValid = true;
            message = "Áp dụng mã SUMMER2026 thành công (-500,000đ)";
        } else {
            // Check Database Discounts
            java.util.Optional<Discount> discountOpt = discountRepository.findByCode(request.getVoucherCode());
            if (discountOpt.isPresent()) {
                Discount discount = discountOpt.get();
                LocalDateTime now = LocalDateTime.now();
                
                // Validate discount
                if (!Boolean.TRUE.equals(discount.getIsActive())) {
                    message = "Mã giảm giá này hiện không còn hoạt động";
                } else if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
                    message = "Chương trình giảm giá chưa bắt đầu";
                } else if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
                    message = "Mã giảm giá đã hết hạn";
                } else if (discount.getUsageLimit() != null && discount.getCurrentUsage() >= discount.getUsageLimit()) {
                    message = "Mã giảm giá đã hết lượt sử dụng";
                } else if (discount.getMinimumBookingAmount() != null && request.getCurrentTotal().compareTo(discount.getMinimumBookingAmount()) < 0) {
                    message = "Đơn hàng chưa đạt giá trị tối thiểu " + discount.getMinimumBookingAmount().longValue() + "đ";
                } else {
                    // Valid!
                    isValid = true;
                    if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                        discountAmount = request.getCurrentTotal().multiply(discount.getValue()).divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
                        message = "Áp dụng mã thành công (-" + discount.getValue() + "%)";
                    } else {
                        discountAmount = discount.getValue();
                        message = "Áp dụng mã thành công (-" + discountAmount.longValue() + "đ)";
                    }
                }
            }
        }
        
        BigDecimal finalTotal = request.getCurrentTotal().subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }
        
        return VoucherResponse.builder()
                .isValid(isValid)
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        booking.setStatus(BookingStatus.CANCELLED);

        TourSchedule schedule = booking.getSchedule();
        if (schedule != null && booking.getNumberOfPeople() != null) {
            schedule.setAvailableSlots(schedule.getAvailableSlots() + booking.getNumberOfPeople());
            tourScheduleRepository.save(schedule);
        }

        bookingRepository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse requestRefund(Long id, RefundRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        booking.setStatus(BookingStatus.REFUND_REQUESTED);
        bookingRepository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    private String normalizeForPdf(String text) {
        if (text == null) return "n/a";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replace("đ", "d").replace("Đ", "D");
    }

    @Override
    public byte[] downloadInvoice(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.newLineAtOffset(50, 770);
                cs.showText("TourBooking - Invoice");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 735);
                cs.showText("Invoice for booking #" + booking.getId());
                cs.newLineAtOffset(0, -18);
                cs.showText("Status: " + normalizeForPdf(String.valueOf(booking.getStatus())));
                cs.newLineAtOffset(0, -18);
                cs.showText("Booking date: " + normalizeForPdf(String.valueOf(booking.getBookingDate())));
                cs.newLineAtOffset(0, -18);
                cs.showText("People: " + String.valueOf(booking.getNumberOfPeople()));
                cs.newLineAtOffset(0, -18);
                cs.showText("Total: " + String.valueOf(booking.getTotalPrice()) + " VND");
                cs.newLineAtOffset(0, -18);
                
                String customerName = "n/a";
                if (booking.getUser() != null) {
                    customerName = normalizeForPdf(booking.getUser().getFullName()) + " (" + booking.getUser().getEmail() + ")";
                }
                cs.showText("Customer: " + customerName);
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Could not generate invoice PDF", e);
        }
    }

    @Override
    @Transactional
    public void generateTestData() {
        User admin = userRepository.findByEmail("admin@gmail.com").orElse(null);
        if (admin == null) {
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) admin = users.get(0);
        }
        
        List<TourSchedule> schedules = tourScheduleRepository.findAll();
        if (schedules.isEmpty() || admin == null) return;
        
        TourSchedule schedule = schedules.get(0);
        
        for (int i = 0; i < 5; i++) {
            Booking booking = new Booking();
            booking.setUser(admin);
            booking.setSchedule(schedule);
            booking.setNumberOfPeople(2);
            BigDecimal price = schedule.getTour() != null ? schedule.getTour().getPrice() : new BigDecimal(1000000);
            booking.setTotalPrice(price.multiply(new BigDecimal(2)));
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setBookingDate(LocalDateTime.now().minusDays(i));
            booking = bookingRepository.save(booking);
            
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(booking.getTotalPrice());
            payment.setPaymentMethod("TEST_DATA");
            payment.setTransactionCode("TEST_" + System.currentTimeMillis() + i);
            payment.setPaymentDate(LocalDateTime.now().minusDays(i));
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
        }
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(Long id, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        try {
            BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
            
            // Nếu chuyển sang CANCELLED mà trước đó chưa phải CANCELLED thì hoàn lại slot
            if (newStatus == BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.CANCELLED) {
                TourSchedule schedule = booking.getSchedule();
                if (schedule != null && booking.getNumberOfPeople() != null) {
                    schedule.setAvailableSlots(schedule.getAvailableSlots() + booking.getNumberOfPeople());
                    tourScheduleRepository.save(schedule);
                }
            }
            
            if ((newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.SUCCESS) 
                    && booking.getStatus() == BookingStatus.PENDING) {
                incrementDiscountUsage(booking);
            }
            
            booking.setStatus(newStatus);
            bookingRepository.save(booking);
            return BookingMapper.toResponse(booking);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void incrementDiscountUsage(Booking booking) {
        if (booking.getDiscountCode() != null && !booking.getDiscountCode().isEmpty()) {
            String code = booking.getDiscountCode().toUpperCase();
            if (!"SUMMER".equals(code) && !"SUMMER2026".equals(code)) {
                discountRepository.findByCode(code).ifPresent(discount -> {
                    discount.setCurrentUsage(discount.getCurrentUsage() + 1);
                    discountRepository.save(discount);
                });
            }
        }
    }
}
