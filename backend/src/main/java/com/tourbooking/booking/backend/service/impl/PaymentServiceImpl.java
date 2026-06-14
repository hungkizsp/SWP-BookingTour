package com.tourbooking.booking.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;
import com.tourbooking.booking.backend.model.dto.response.VNPayConfirmResponse;
import com.tourbooking.booking.backend.service.VNPayService;
import com.tourbooking.booking.backend.config.VNPayConfig;
import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.Payment;
import com.tourbooking.booking.backend.model.entity.PaymentLog;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.PaymentStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.PaymentLogRepository;
import com.tourbooking.booking.backend.repository.PaymentRepository;
import com.tourbooking.booking.backend.service.LoyaltyService;
import com.tourbooking.booking.backend.service.MailService;
import com.tourbooking.booking.backend.service.PayOSService;
import com.tourbooking.booking.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import com.tourbooking.booking.backend.repository.DiscountRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final int PAYOS_CONFIRM_MAX_RETRIES = 3;
    private static final long PAYOS_CONFIRM_RETRY_DELAY_MS = 1500L;

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final LoyaltyService loyaltyService;
    private final PayOSService payOSService;
    private final VNPayService vnPayService;
    private final PayOS payOS;
    private final MailService mailService;
    private final ObjectMapper objectMapper;
    private final DiscountRepository discountRepository;

    @Override
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionCode(request.getTransactionCode());
        payment.setPaymentDate(LocalDateTime.now());

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
            payment.setStatus(PaymentStatus.PENDING);
        } else {
            payment.setAmount(booking.getTotalPrice());
            payment.setStatus(PaymentStatus.SUCCESS);
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.PENDING_CASH) {
                incrementDiscountUsage(booking);
            }
            booking.setStatus(BookingStatus.CONFIRMED);
            awardLoyaltyAndSendMail(booking, payment.getAmount());
        }

        Payment saved = paymentRepository.save(payment);
        bookingRepository.save(booking);
        savePaymentLog(saved, "Manual payment created");

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse createPayOSPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        BigDecimal payAmount = request.getAmount() != null ? request.getAmount() : booking.getTotalPrice();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        long orderCode = booking.getId() * 1000 + (System.currentTimeMillis() % 1000);
        // hoặc đơn giản: long orderCode = System.currentTimeMillis() / 1000;
        String transactionCode = "PAYOS-" + orderCode;
        int amount = payAmount.intValue();

        String checkoutUrl = payOSService.createPaymentLink(
                orderCode,
                amount,
                "Booking #" + booking.getId());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(payAmount);
        payment.setPaymentMethod("PAYOS");
        payment.setTransactionCode(transactionCode);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        savePaymentLog(saved, "PayOS link created");

        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .bookingId(booking.getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus().name())
                .checkoutUrl(checkoutUrl)
                .orderCode(orderCode)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse createCashPaymentIntent(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentMethod("CASH");
        payment.setTransactionCode("CASH-" + System.currentTimeMillis());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        
        booking.setStatus(BookingStatus.PENDING_CASH);
        bookingRepository.save(booking);
        
        savePaymentLog(saved, "Cash payment intent created");

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse createVNPayPayment(Long bookingId, HttpServletRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        BigDecimal payAmount = booking.getTotalPrice();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String txnRef = bookingId + "_" + System.currentTimeMillis();
        String transactionCode = "VNPAY-" + txnRef;
        long vnpAmount = payAmount.multiply(BigDecimal.valueOf(100)).longValue();

        String checkoutUrl = vnPayService.createPaymentUrl(
                txnRef,
                vnpAmount,
                "Thanh toan dat tour #" + bookingId,
                VNPayConfig.getIpAddress(request));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(payAmount);
        payment.setPaymentMethod("VNPAY");
        payment.setTransactionCode(transactionCode);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        savePaymentLog(saved, "VNPay link created");

        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .bookingId(bookingId)
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus().name())
                .checkoutUrl(checkoutUrl)
                .build();
    }

    @Override
    @Transactional
    public VNPayConfirmResponse confirmVNPayReturn(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (!vnPayService.validateReturn(params)) {
            throw new AppException(ErrorCode.VNPAY_SIGNATURE_INVALID);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionCode = "VNPAY-" + txnRef;

        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (!vnPayAmountMatches(payment, params.get("vnp_Amount"))) {
            savePaymentLog(payment, "VNPay return: amount mismatch");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Long bookingId = parseBookingIdFromTxnRef(txnRef);

        if ("00".equals(responseCode)) {
            finalizePayOsPaymentSuccess(payment, "VNPay return: ResponseCode=00");
            Payment refreshed = paymentRepository.findById(payment.getId()).orElse(payment);
            return VNPayConfirmResponse.builder()
                    .success(true)
                    .message("Thanh toán VNPay thành công")
                    .bookingId(bookingId)
                    .transactionRef(txnRef)
                    .responseCode(responseCode)
                    .payment(toResponse(refreshed))
                    .build();
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            savePaymentLog(payment, "VNPay return failed: ResponseCode=" + responseCode);
        }

        return VNPayConfirmResponse.builder()
                .success(false)
                .message("Thanh toán VNPay không thành công (mã: " + responseCode + ")")
                .bookingId(bookingId)
                .transactionRef(txnRef)
                .responseCode(responseCode)
                .payment(toResponse(payment))
                .build();
    }

    private static Long parseBookingIdFromTxnRef(String txnRef) {
        if (txnRef == null || !txnRef.contains("_")) {
            return null;
        }
        try {
            return Long.parseLong(txnRef.substring(0, txnRef.indexOf('_')));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean vnPayAmountMatches(Payment payment, String vnpAmount) {
        if (vnpAmount == null || vnpAmount.isBlank() || payment.getAmount() == null) {
            return true;
        }
        try {
            long expected = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            return expected == Long.parseLong(vnpAmount.trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(String rawPayload, String headerSignature) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        JsonNode data = root.path("data");
        String signature = (headerSignature != null && !headerSignature.isBlank())
                ? headerSignature.trim()
                : root.path("signature").asText("").trim();
        if (!payOSService.verifyPayOsDataSignature(data, signature)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (!data.isObject() || !data.has("orderCode") || data.get("orderCode").isNull()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        long orderCode = data.path("orderCode").asLong();
        String transactionCode = "PAYOS-" + orderCode;
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (!PayOSService.amountsMatch(payment.getAmount(), data)) {
            savePaymentLog(payment, "PayOS webhook: amount mismatch — không cập nhật");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (isPayOsPaidFromWebhook(root, data)) {
            finalizePayOsPaymentSuccess(payment, "PayOS webhook (VietQR) thành công");
            return;
        }

        if (isPayOsCancelledOrFailedFromWebhook(root, data)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                savePaymentLog(payment, "PayOS webhook: hủy / thất bại");
            }
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayOsAfterReturn(long orderCode) {
        AppException lastPending = null;
        for (int attempt = 1; attempt <= PAYOS_CONFIRM_MAX_RETRIES; attempt++) {
            try {
                return confirmPayOsAfterReturnOnce(orderCode, attempt);
            } catch (AppException ex) {
                if (ex.getErrorCode() == ErrorCode.PAYOS_PAYMENT_PENDING) {
                    lastPending = ex;
                    if (attempt < PAYOS_CONFIRM_MAX_RETRIES) {
                        log.info("PayOS confirm orderCode={} still PENDING (attempt {}/{}), retrying...",
                                orderCode, attempt, PAYOS_CONFIRM_MAX_RETRIES);
                        sleepQuietly(PAYOS_CONFIRM_RETRY_DELAY_MS);
                        continue;
                    }
                }
                throw ex;
            }
        }
        throw lastPending != null ? lastPending : new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
    }

    @Override
    @Transactional
    public int reconcilePendingPayOsPaymentsInRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Payment> pendingPayments = paymentRepository.findPendingPayOsInRange(
                PaymentStatus.PENDING, "PAYOS", start, end);

        int synced = 0;
        for (Payment payment : pendingPayments) {
            try {
                if (reconcileSinglePendingPayOsPayment(payment)) {
                    synced++;
                }
            } catch (Exception e) {
                log.warn("PayOS reconciliation skipped for payment #{}: {}",
                        payment.getId(), e.getMessage());
            }
        }

        log.info("PayOS pre-report reconciliation: {}/{} pending payments synced to SUCCESS",
                synced, pendingPayments.size());
        return synced;
    }

    private PaymentResponse confirmPayOsAfterReturnOnce(long orderCode, int attempt) {
        String payOsStatus = fetchPayOsRemoteStatus(orderCode);
        if (payOsStatus == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String transactionCode = "PAYOS-" + orderCode;
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (!remoteAmountMatchesPayment(payment, orderCode)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if ("PAID".equals(payOsStatus)) {
            finalizePayOsPaymentSuccess(payment,
                    "PayOS returnUrl + API: PAID (attempt " + attempt + ")");
            return toResponse(paymentRepository.findById(payment.getId()).orElse(payment));
        }
        if ("PENDING".equals(payOsStatus) || "PROCESSING".equals(payOsStatus)) {
            throw new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
        }
        if ("CANCELLED".equals(payOsStatus) || "FAILED".equals(payOsStatus)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                savePaymentLog(payment, "PayOS API: " + payOsStatus);
            }
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private boolean reconcileSinglePendingPayOsPayment(Payment payment) {
        Long orderCode = parsePayOsOrderCode(payment);
        if (orderCode == null) {
            return false;
        }

        String payOsStatus = fetchPayOsRemoteStatus(orderCode);
        if (payOsStatus == null) {
            return false;
        }

        if (!remoteAmountMatchesPayment(payment, orderCode)) {
            log.warn("PayOS reconciliation amount mismatch for payment #{}", payment.getId());
            return false;
        }

        if ("PAID".equals(payOsStatus)) {
            finalizePayOsPaymentSuccess(payment, "PayOS active reconciliation before financial report: PAID");
            return true;
        }

        if ("CANCELLED".equals(payOsStatus) || "FAILED".equals(payOsStatus)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            savePaymentLog(payment, "PayOS reconciliation: " + payOsStatus);
        }
        return false;
    }

    /**
     * Gọi PayOS SDK (getPaymentLinkInformation), fallback REST nếu SDK lỗi.
     */
    private String fetchPayOsRemoteStatus(long orderCode) {
        try {
            PaymentLinkData linkData = payOS.getPaymentLinkInformation(orderCode);
            if (linkData != null && linkData.getStatus() != null && !linkData.getStatus().isBlank()) {
                return linkData.getStatus().trim().toUpperCase(Locale.ROOT);
            }
        } catch (Exception e) {
            log.warn("PayOS SDK getPaymentLinkInformation({}) failed: {}", orderCode, e.getMessage());
        }

        Optional<JsonNode> apiDataOpt = payOSService.fetchPaymentRequestByOrderCode(orderCode);
        return apiDataOpt
                .map(d -> d.path("status").asText("").trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .orElse(null);
    }

    private boolean remoteAmountMatchesPayment(Payment payment, long orderCode) {
        try {
            PaymentLinkData linkData = payOS.getPaymentLinkInformation(orderCode);
            if (linkData != null && linkData.getAmount() != null && payment.getAmount() != null) {
                return payment.getAmount().compareTo(BigDecimal.valueOf(linkData.getAmount())) == 0;
            }
        } catch (Exception ignored) {
            // fallback REST below
        }

        Optional<JsonNode> apiDataOpt = payOSService.fetchPaymentRequestByOrderCode(orderCode);
        return apiDataOpt.map(d -> PayOSService.amountsMatch(payment.getAmount(), d)).orElse(true);
    }

    private static Long parsePayOsOrderCode(Payment payment) {
        String transactionCode = payment.getTransactionCode();
        if (transactionCode == null || !transactionCode.startsWith("PAYOS-")) {
            return null;
        }
        try {
            return Long.parseLong(transactionCode.substring("PAYOS-".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmManualPayment(PaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Payment payment = paymentRepository
                .findFirstByBooking_IdAndStatusOrderByPaymentDateDesc(booking.getId(), PaymentStatus.PENDING)
                .or(() -> paymentRepository.findFirstByBooking_IdOrderByPaymentDateDesc(booking.getId()))
                .orElseGet(() -> {
                    Payment created = new Payment();
                    created.setBooking(booking);
                    created.setAmount(booking.getTotalPrice());
                    created.setPaymentMethod(
                            request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                                    ? request.getPaymentMethod()
                                    : "BANK_TRANSFER");
                    created.setTransactionCode("MANUAL-" + System.currentTimeMillis());
                    created.setStatus(PaymentStatus.PENDING);
                    created.setPaymentDate(LocalDateTime.now());
                    Payment saved = paymentRepository.save(created);
                    savePaymentLog(saved, "Manual payment created on confirm flow");
                    return saved;
                });

        boolean isNewlyPaid = false;
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
            isNewlyPaid = true;
        }

        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.PENDING_CASH) {
            incrementDiscountUsage(booking);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            
            if (isNewlyPaid) {
                awardLoyaltyAndSendMail(booking, payment.getAmount());
                savePaymentLog(payment, "Manual transfer confirmed by operator");
            }
        } else if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS && booking.getStatus() != BookingStatus.COMPLETED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment saved) {
        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .bookingId(saved.getBooking().getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus().name())
                .build();
    }

    private void savePaymentLog(Payment payment, String message) {
        PaymentLog log = new PaymentLog();
        log.setPayment(payment);
        log.setLogMessage(message + " | amount=" + payment.getAmount() + " | method=" + payment.getPaymentMethod());
        paymentLogRepository.save(log);
    }

    private void awardLoyaltyAndSendMail(Booking booking, BigDecimal paidAmount) {
        if (booking.getUser() != null && paidAmount != null) {
            loyaltyService.addPoint(booking.getUser().getId(), paidAmount.intValue() / 100000);
            try {
                mailService.sendPaymentSuccessEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getFullName(),
                        booking.getId(),
                        paidAmount);
            } catch (Exception e) {
                log.error("Failed to send payment success email for booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    private void finalizePayOsPaymentSuccess(Payment payment, String logMessage) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.PENDING_CASH) {
            incrementDiscountUsage(booking);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        paymentRepository.save(payment);
        awardLoyaltyAndSendMail(booking, payment.getAmount());
        savePaymentLog(payment, logMessage);
    }

    private static boolean isPayOsPaidFromWebhook(JsonNode root, JsonNode data) {
        boolean outerOk = "00".equals(root.path("code").asText()) && root.path("success").asBoolean(false);
        if (!outerOk) {
            return false;
        }
        String dataStatus = data.path("status").asText("");
        if ("PAID".equalsIgnoreCase(dataStatus) || "SUCCESS".equalsIgnoreCase(dataStatus)) {
            return true;
        }
        String innerCode = data.path("code").asText("");
        return innerCode.isEmpty() || "00".equals(innerCode);
    }

    private static boolean isPayOsCancelledOrFailedFromWebhook(JsonNode root, JsonNode data) {
        if (!root.path("success").asBoolean(true)) {
            return true;
        }
        String dataStatus = data.path("status").asText("");
        return "CANCELLED".equalsIgnoreCase(dataStatus) || "FAILED".equalsIgnoreCase(dataStatus);
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