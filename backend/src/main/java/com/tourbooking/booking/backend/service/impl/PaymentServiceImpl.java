package com.tourbooking.booking.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.PaymentRequest;
import com.tourbooking.booking.backend.model.dto.response.PaymentResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final LoyaltyService loyaltyService;
    private final PayOSService payOSService;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

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
        Optional<JsonNode> apiDataOpt = payOSService.fetchPaymentRequestByOrderCode(orderCode);
        if (apiDataOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        JsonNode d = apiDataOpt.get();
        String status = d.path("status").asText("").trim().toUpperCase(Locale.ROOT);
        String transactionCode = "PAYOS-" + orderCode;
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (!PayOSService.amountsMatch(payment.getAmount(), d)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if ("PAID".equals(status)) {
            finalizePayOsPaymentSuccess(payment, "PayOS returnUrl + API: PAID");
            return toResponse(paymentRepository.findById(payment.getId()).orElse(payment));
        }
        if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
            throw new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
        }
        if ("CANCELLED".equals(status) || "FAILED".equals(status)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                savePaymentLog(payment, "PayOS API: " + status);
            }
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
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

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
            awardLoyaltyAndSendMail(booking, payment.getAmount());
            savePaymentLog(payment, "Manual transfer confirmed by operator");
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
            mailService.sendPaymentSuccessEmail(
                    booking.getUser().getEmail(),
                    booking.getUser().getFullName(),
                    booking.getId(),
                    paidAmount);
        }
    }

    private void finalizePayOsPaymentSuccess(Payment payment, String logMessage) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        Booking booking = payment.getBooking();
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
}