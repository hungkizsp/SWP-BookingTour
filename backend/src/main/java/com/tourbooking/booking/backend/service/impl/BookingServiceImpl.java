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
import com.tourbooking.booking.backend.service.PassengerClassificationService;
import com.tourbooking.booking.backend.service.PaymentService;
import com.tourbooking.booking.backend.service.TourScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
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
    private final com.tourbooking.booking.backend.repository.RefundRequestRepository refundRequestRepository;
    private final PassengerClassificationService passengerClassificationService;
    private final TourScheduleService tourScheduleService;
    private final PaymentService paymentService;
    private final Environment environment;
    private final com.tourbooking.booking.backend.service.MailService mailService;

    private final com.tourbooking.booking.backend.repository.DiscountPolicyRepository discountPolicyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<Long, com.tourbooking.booking.backend.model.entity.RefundRequest> refundMap = loadLatestRefundsByBookingId(
                bookings.stream().map(Booking::getId).toList());
        return bookings.stream()
                .map(booking -> {
                    BookingResponse response = BookingMapper.toResponse(booking);
                    enrichRefundInfo(response, booking.getId(), refundMap);
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BookingResponse> getAllBookingsPaginated(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Booking> bookingPage = bookingRepository.findAll(pageable);
        List<Booking> bookings = bookingPage.getContent();
        Map<Long, com.tourbooking.booking.backend.model.entity.RefundRequest> refundMap = loadLatestRefundsByBookingId(
                bookings.stream().map(Booking::getId).toList());
        
        return bookingPage.map(booking -> {
            BookingResponse response = BookingMapper.toResponse(booking);
            enrichRefundInfo(response, booking.getId(), refundMap);
            return response;
        });
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

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
            boolean isStaffOrAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            if (!isStaffOrAdmin) {
                User currentUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                if (!booking.getUser().getId().equals(currentUser.getId())) {
                    throw new AppException(ErrorCode.FORBIDDEN);
                }
            }
        }

        BookingResponse response = BookingMapper.toResponse(booking);
        enrichRefundInfo(response, id, null);
        return response;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TourSchedule schedule = tourScheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        int declaredAdultCount = request.getAdultCount() != null ? request.getAdultCount() : 1;
        int declaredChildCount = request.getChildCount() != null ? request.getChildCount() : 0;
        int declaredInfantCount = request.getInfantCount() != null ? request.getInfantCount() : 0;
        LocalDate tourStartDate = schedule.getStartDate();

        PassengerClassificationService.ClassificationResult classification = passengerClassificationService.classify(
                request.getPassengers(),
                tourStartDate,
                declaredAdultCount,
                declaredChildCount,
                declaredInfantCount,
                schedule.getMaxSlots());

        int slotsToDeduct = classification.getSlotsToDeduct();
        BigDecimal tourPrice = schedule.getTour().getPrice();
        BigDecimal totalPrice = calculatePassengerTotalPrice(
                tourPrice,
                classification.getRealAdultCount(),
                classification.getRealChildCount(),
                classification.getRealInfantCount());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setNumberOfPeople(classification.getTotalPassengers());
        booking.setOccupiedSlots(slotsToDeduct);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingDate(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        tourScheduleService.deductAvailableSlots(schedule.getId(), slotsToDeduct);

        for (PassengerClassificationService.ClassifiedPassenger classified : classification.getPassengers()) {
            PassengerRequest pr = classified.getRequest();
            Passenger passenger = new Passenger();
            passenger.setBooking(saved);
            passenger.setFullName(pr.getFullName());
            passenger.setDateOfBirth(pr.getDateOfBirth());
            passenger.setIdNumber(pr.getIdNumber());
            passenger.setPassengerType(classified.getPassengerType().name());
            passengerRepository.save(passenger);
        }

        applyDiscountIfPresent(saved, request.getDiscountCode());

        Booking savedBooking = bookingRepository.save(saved);
        
        try {
            mailService.sendBookingCreatedEmail(user.getEmail(), user.getFullName(), savedBooking.getId(), savedBooking.getTotalPrice());
        } catch (Exception e) {
            log.warn("Could not send booking creation email: " + e.getMessage());
        }
        
        return BookingMapper.toResponse(savedBooking);
    }

    private BigDecimal calculatePassengerTotalPrice(
            BigDecimal tourPrice,
            int realAdultCount,
            int realChildCount,
            int realInfantCount) {

        BigDecimal childRate = discountPolicyRepository.findByPassengerType("CHILD")
                .filter(com.tourbooking.booking.backend.model.entity.DiscountPolicy::getIsActive)
                .map(com.tourbooking.booking.backend.model.entity.DiscountPolicy::getRate)
                .orElse(new BigDecimal("0.75"));

        BigDecimal infantRate = discountPolicyRepository.findByPassengerType("INFANT")
                .filter(com.tourbooking.booking.backend.model.entity.DiscountPolicy::getIsActive)
                .map(com.tourbooking.booking.backend.model.entity.DiscountPolicy::getRate)
                .orElse(new BigDecimal("0.10"));

        return tourPrice.multiply(BigDecimal.valueOf(realAdultCount))
                .add(tourPrice.multiply(childRate).multiply(BigDecimal.valueOf(realChildCount)))
                .add(tourPrice.multiply(infantRate).multiply(BigDecimal.valueOf(realInfantCount)));
    }

    private void applyDiscountIfPresent(Booking saved, String discountCode) {
        if (discountCode == null || discountCode.isEmpty()) {
            return;
        }

        String code = discountCode.toUpperCase();
        BigDecimal discountAmt = BigDecimal.ZERO;
        boolean applied = false;

        if ("SUMMER".equals(code)) {
            discountAmt = saved.getTotalPrice().multiply(new BigDecimal("20"))
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            saved.setDiscountCode("SUMMER");
            applied = true;
        } else {
            Discount discount = discountRepository.findByCode(code).orElse(null);
            if (discount != null && discount.getIsActive()
                    && (discount.getStartDate() == null || !LocalDateTime.now().isBefore(discount.getStartDate()))
                    && (discount.getEndDate() == null || !LocalDateTime.now().isAfter(discount.getEndDate()))
                    && (discount.getUsageLimit() == null || discount.getCurrentUsage() < discount.getUsageLimit())) {

                if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                    discountAmt = saved.getTotalPrice().multiply(discount.getValue())
                            .divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
                } else {
                    discountAmt = discount.getValue();
                }
                saved.setDiscountCode(discount.getCode());
                applied = true;
            }
        }

        if (applied) {
            saved.setDiscountAmount(discountAmt);
            saved.setTotalPrice(saved.getTotalPrice().subtract(discountAmt));
        }
    }

    private int resolveOccupiedSlots(Booking booking) {
        if (booking.getOccupiedSlots() != null) {
            return booking.getOccupiedSlots();
        }
        return booking.getNumberOfPeople() != null ? booking.getNumberOfPeople() : 0;
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

            int occupied = resolveOccupiedSlots(existingBooking);

            // check slot schedule mới
            if (newSchedule.getAvailableSlots() < occupied) {
                throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
            }

            // trả slot schedule cũ
            oldSchedule.setAvailableSlots(oldSchedule.getAvailableSlots() + occupied);

            // trừ slot schedule mới
            newSchedule.setAvailableSlots(newSchedule.getAvailableSlots() - occupied);

            tourScheduleRepository.save(oldSchedule);
            tourScheduleRepository.save(newSchedule);

            existingBooking.setSchedule(newSchedule);
        }
        // handle change occupied slots (Adults + Children ONLY, exclude Infants)
        if (request.getAdultCount() != null || request.getChildCount() != null || request.getOccupiedSlots() > 0) {
            int oldSlots = resolveOccupiedSlots(existingBooking);
            int newSlots = oldSlots;

            if (request.getAdultCount() != null || request.getChildCount() != null) {
                int a = request.getAdultCount() != null ? request.getAdultCount() : 0;
                int c = request.getChildCount() != null ? request.getChildCount() : 0;
                newSlots = a + c;
            } else if (request.getOccupiedSlots() > 0) {
                newSlots = request.getOccupiedSlots();
            }

            int slotDiff = newSlots - oldSlots;

            if (slotDiff != 0) {
                TourSchedule schedule = tourScheduleRepository.findByIdWithLock(existingBooking.getSchedule().getId())
                        .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

                if (slotDiff > 0 && schedule.getAvailableSlots() < slotDiff) {
                    throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
                }

                schedule.setAvailableSlots(schedule.getAvailableSlots() - slotDiff);
                tourScheduleRepository.save(schedule);
                existingBooking.setOccupiedSlots(newSlots);
            }
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
    @Transactional
    public List<FinancialReportResponse> getFinancialReport(
            String start, String end, String type, String status, boolean includeTest) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        boolean devProfile = isDevProfile();
        boolean includeTestData = includeTest || "INCLUDE_TEST".equalsIgnoreCase(status);

        log.info("Generating financial report {} to {}, type={}, status={}, includeTest={}, devProfile={}",
                startDate, endDate, type, status, includeTestData, devProfile);

        try {
            int synced = paymentService.reconcilePendingPayOsPaymentsInRange(startDate, endDate);
            log.info("Pre-report PayOS reconciliation updated {} payment(s) to SUCCESS", synced);
        } catch (Exception e) {
            log.warn("Pre-report PayOS reconciliation failed (report continues): {}", e.getMessage());
        }

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(23, 59, 59, 999_999_999);

        List<Payment> paymentsInRange = paymentRepository.findInDateRange(rangeStart, rangeEnd);
        log.info("Payments in date range {} - {}: {}", startDate, endDate, paymentsInRange.size());

        List<Payment> filteredPayments = paymentsInRange.stream()
                .filter(p -> matchesFinancialReportPayment(p, status, includeTestData, devProfile))
                .toList();

        log.info("Filtered payments for report count: {}", filteredPayments.size());

        // Lấy bookings bị CANCELLED trong khoảng thời gian (để tính cancellation rate)
        List<Booking> cancelledBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .filter(b -> isBookingWithinRange(b, startDate, endDate))
                .toList();

        // Formatter theo loại báo cáo
        DateTimeFormatter formatter = switch (type.toLowerCase()) {
            case "weekly" -> DateTimeFormatter.ofPattern("yyyy-'W'ww", Locale.getDefault());
            case "monthly" -> DateTimeFormatter.ofPattern("yyyy-MM");
            case "yearly" -> DateTimeFormatter.ofPattern("yyyy");
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // Group payments theo period
        Map<String, List<Payment>> groupedPayments = filteredPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            LocalDateTime date = p.getPaymentDate() != null ? p.getPaymentDate() : p.getCreatedAt();
                            return date != null ? date.format(formatter) : "Unknown";
                        },
                        TreeMap::new,
                        Collectors.toList()));

        // Group cancellations theo period
        Map<String, Long> cancelledByPeriod = cancelledBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> {
                            LocalDateTime date = b.getCreatedAt() != null ? b.getCreatedAt() : b.getUpdatedAt();
                            return date != null ? date.format(formatter) : "Unknown";
                        },
                        Collectors.counting()));

        // Nếu không có payment nào, trả về danh sách rỗng với log warning
        if (groupedPayments.isEmpty()) {
            log.warn("No SUCCESS payments found in range {} - {}", start, end);
        }

        return groupedPayments.entrySet().stream()
                .map(entry -> {
                    String period = entry.getKey();
                    List<Payment> periodPayments = entry.getValue();

                    long bookingCount = periodPayments.size();

                    // Revenue = tổng Payment.amount của các payment SUCCESS, nếu REFUNDED thì tính là giá trị âm
                    BigDecimal revenue = periodPayments.stream()
                            .map(p -> {
                                if (p.getAmount() == null) return BigDecimal.ZERO;
                                boolean isRefund = p.getStatus() == PaymentStatus.REFUNDED ||
                                                   (p.getBooking() != null && p.getBooking().getStatus() == BookingStatus.REFUNDED);
                                return isRefund ? p.getAmount().negate() : p.getAmount();
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long cancellations = cancelledByPeriod.getOrDefault(period, 0L);

                    BigDecimal averageValue = bookingCount > 0
                            ? revenue.divide(BigDecimal.valueOf(bookingCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    log.info("Period: {}, bookings: {}, revenue: {}, cancellations: {}", period, bookingCount, revenue,
                            cancellations);

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

    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return true;
        }
        for (String profile : profiles) {
            if ("dev".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lọc payment cho báo cáo tài chính.
     * Cập nhật mới: Lấy TẤT CẢ các trạng thái payment. 
     */
    private boolean matchesFinancialReportPayment(
            Payment payment, String statusFilter, boolean includeTestData, boolean devProfile) {
        boolean isTestData = "TEST_DATA".equals(payment.getPaymentMethod());

        // Nếu status filter khác rỗng và không phải là "all" hay "COMPLETED", 
        // ta có thể filter theo request nếu cần. Mặc định giờ báo cáo muốn all.
        if (!includeTestData && isTestData) {
            return false;
        }

        return true;
    }

    private boolean isBookingWithinRange(Booking booking, LocalDate startDate, LocalDate endDate) {
        LocalDateTime bookingDateTime = booking.getCreatedAt() != null
                ? booking.getCreatedAt()
                : booking.getUpdatedAt();
        if (bookingDateTime == null) {
            return false;
        }
        LocalDate bookingDate = bookingDateTime.toLocalDate();
        return !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate);
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
        LocalDateTime rangeStart = current.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = current.atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        boolean devProfile = isDevProfile();

        return paymentRepository.findInDateRange(rangeStart, rangeEnd).stream()
                .filter(p -> matchesFinancialReportPayment(p, "INCLUDE_TEST", true, devProfile))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public VoucherResponse applyVoucher(VoucherRequest request) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        String message = "Mã giảm giá không hợp lệ hoặc đã hết hạn";
        boolean isValid = false;

        // Check for specific hardcoded voucher first (legacy)
        if ("SUMMER".equalsIgnoreCase(request.getVoucherCode())) {
            discountAmount = request.getCurrentTotal().multiply(new BigDecimal("20")).divide(new BigDecimal("100"), 0,
                    RoundingMode.HALF_UP);
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
                } else if (discount.getMinimumBookingAmount() != null
                        && request.getCurrentTotal().compareTo(discount.getMinimumBookingAmount()) < 0) {
                    message = "Đơn hàng chưa đạt giá trị tối thiểu " + discount.getMinimumBookingAmount().longValue()
                            + "đ";
                } else {
                    // Valid!
                    isValid = true;
                    if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                        discountAmount = request.getCurrentTotal().multiply(discount.getValue())
                                .divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
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
        if (schedule != null) {
            tourScheduleService.releaseAvailableSlots(schedule.getId(), resolveOccupiedSlots(booking));
        }

        bookingRepository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    /**
     * 2.1 — Khách hàng gửi yêu cầu hoàn tiền.
     * <ul>
     * <li>Tính refundAmount theo chính sách ngày khởi hành.</li>
     * <li>Cập nhật Booking.status = REFUND_REQUESTED.</li>
     * <li>Insert bản ghi mới vào bảng RefundRequests.</li>
     * </ul>
     */
    @Override
    @Transactional
    public BookingResponse requestRefund(Long id, RefundRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể yêu cầu hoàn tiền với đơn ở trạng thái CONFIRMED hoặc SUCCESS.");
        }

        // ── Tính số tiền hoàn theo chính sách ngày khởi hành ─────────────────
        BigDecimal refundAmount = calculateRefundAmount(booking);

        // Lưu vết trạng thái gốc trước khi cập nhật
        BookingStatus originalStatus = booking.getStatus();

        // ── Cập nhật trạng thái Booking ───────────────────────────────────────
        booking.setStatus(BookingStatus.REFUND_REQUESTED);
        bookingRepository.save(booking);

        // ── Tạo chuỗi Reason chứa thông tin ngân hàng + lý do khách ─────────
        String bankInfo = String.format("Ngân hàng: %s | STK: %s | Chủ TK: %s",
                request.getBankName() != null ? request.getBankName() : "N/A",
                request.getAccountNumber() != null ? request.getAccountNumber() : "N/A",
                request.getAccountHolderName() != null ? request.getAccountHolderName() : "N/A");
        String fullReason = bankInfo + " | Lý do: " + (request.getReason() != null ? request.getReason() : "Không có");

        // ── Insert RefundRequest vào DB ───────────────────────────────────────
        com.tourbooking.booking.backend.model.entity.RefundRequest refundEntity = new com.tourbooking.booking.backend.model.entity.RefundRequest();
        refundEntity.setBooking(booking);
        refundEntity.setAmount(refundAmount);
        refundEntity.setReason(fullReason);
        refundEntity.setStatus(com.tourbooking.booking.backend.model.entity.enums.RefundStatus.PENDING);
        refundEntity.setOriginalBookingStatus(originalStatus); // Lưu trạng thái gốc
        refundRequestRepository.save(refundEntity);

        log.info("[Refund] BookingID={} | RefundAmount={} | OriginalStatus={} | Reason={}",
                id, refundAmount, originalStatus, fullReason);
        return BookingMapper.toResponse(booking);
    }

    /**
     * 2.2 — Tính số tiền hoàn trả theo chính sách:
     * <ul>
     * <li>> 7 ngày trước khởi hành → hoàn 100%</li>
     * <li>3–7 ngày trước khởi hành → hoàn 50%</li>
     * <li>&lt; 3 ngày trước khởi hành → hoàn 0%</li>
     * </ul>
     * Sử dụng múi giờ Việt Nam (Asia/Ho_Chi_Minh) để tránh lệch ngày khi deploy
     * cloud.
     */
    private BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getTotalPrice() == null)
            return BigDecimal.ZERO;

        java.time.LocalDate startDate = (booking.getSchedule() != null)
                ? booking.getSchedule().getStartDate()
                : null;

        if (startDate == null) {
            log.warn("[Refund] BookingID={} không tìm thấy StartDate → hoàn 100%", booking.getId());
            return booking.getTotalPrice();
        }

        // Ep múi giờ Việt Nam — tính ngày hiện tại theo giờ VN dù server ở bất kỳ đâu
        java.time.ZoneId vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.LocalDate today = java.time.LocalDate.now(vnZone);

        long daysUntilDeparture = java.time.temporal.ChronoUnit.DAYS.between(today, startDate);

        log.info("[Refund] BookingID={} | StartDate={} | TodayVN={} | DaysLeft={}",
                booking.getId(), startDate, today, daysUntilDeparture);

        if (daysUntilDeparture > 7) {
            return booking.getTotalPrice(); // Hoàn 100%
        } else if (daysUntilDeparture >= 3) {
            return booking.getTotalPrice()
                    .multiply(new BigDecimal("0.50"))
                    .setScale(2, RoundingMode.HALF_UP); // Hoàn 50%
        } else {
            return BigDecimal.ZERO; // Hoàn 0%
        }
    }

    private String normalizeForPdf(String text) {
        if (text == null)
            return "n/a";
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
                    customerName = normalizeForPdf(booking.getUser().getFullName()) + " ("
                            + booking.getUser().getEmail() + ")";
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
            if (!users.isEmpty())
                admin = users.get(0);
        }

        List<TourSchedule> schedules = tourScheduleRepository.findAll();
        if (schedules.isEmpty() || admin == null)
            return;

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
                if (schedule != null) {
                    tourScheduleService.releaseAvailableSlots(schedule.getId(), resolveOccupiedSlots(booking));
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

    private Map<Long, com.tourbooking.booking.backend.model.entity.RefundRequest> loadLatestRefundsByBookingId(
            List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Map.of();
        }
        return refundRequestRepository.findByBooking_IdIn(bookingIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getBooking().getId(),
                        r -> r,
                        (existing, incoming) -> {
                            LocalDateTime existingAt = existing.getCreatedAt() != null
                                    ? existing.getCreatedAt()
                                    : LocalDateTime.MIN;
                            LocalDateTime incomingAt = incoming.getCreatedAt() != null
                                    ? incoming.getCreatedAt()
                                    : LocalDateTime.MIN;
                            return incomingAt.isAfter(existingAt) ? incoming : existing;
                        }));
    }

    private void enrichRefundInfo(
            BookingResponse response,
            Long bookingId,
            Map<Long, com.tourbooking.booking.backend.model.entity.RefundRequest> refundMap) {
        com.tourbooking.booking.backend.model.entity.RefundRequest refund = refundMap != null
                ? refundMap.get(bookingId)
                : refundRequestRepository.findTopByBooking_IdOrderByCreatedAtDesc(bookingId).orElse(null);
        if (refund == null) {
            return;
        }
        response.setRefundReason(refund.getReason());
        response.setRefundStatus(refund.getStatus() != null ? refund.getStatus().name() : null);
        response.setRefundAmount(refund.getAmount());
    }
}
