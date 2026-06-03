package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.dto.request.DiscountRequest;
import com.tourbooking.booking.backend.model.dto.response.DiscountResponse;
import com.tourbooking.booking.backend.model.entity.Discount;
import com.tourbooking.booking.backend.model.entity.enums.DiscountType;
import com.tourbooking.booking.backend.repository.DiscountRepository;
import com.tourbooking.booking.backend.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DiscountResponse getDiscountById(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discount not found"));
        return mapToResponse(discount);
    }

    @Override
    public DiscountResponse createDiscount(DiscountRequest request) {
        Discount discount = new Discount();
        updateEntity(discount, request);
        return mapToResponse(discountRepository.save(discount));
    }

    @Override
    public DiscountResponse updateDiscount(Long id, DiscountRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discount not found"));
        updateEntity(discount, request);
        return mapToResponse(discountRepository.save(discount));
    }

    @Override
    public void deleteDiscount(Long id) {
        discountRepository.deleteById(id);
    }

    @Override
    public DiscountResponse validateCode(String code, BigDecimal amount) {
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        if (!discount.getIsActive()) {
            throw new RuntimeException("Mã giảm giá đã bị vô hiệu hóa");
        }

        LocalDateTime now = LocalDateTime.now();
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new RuntimeException("Mã giảm giá chưa đến hạn sử dụng");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng");
        }

        if (discount.getUsageLimit() != null && discount.getCurrentUsage() >= discount.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
        }

        if (discount.getMinimumBookingAmount() != null && amount.compareTo(discount.getMinimumBookingAmount()) < 0) {
            throw new RuntimeException("Giá trị đơn hàng chưa đủ để áp dụng mã này (Tối thiểu " + discount.getMinimumBookingAmount() + ")");
        }

        return mapToResponse(discount);
    }

    private void updateEntity(Discount entity, DiscountRequest request) {
        entity.setCode(request.getCode());
        entity.setDiscountType(request.getDiscountType());
        entity.setValue(request.getValue());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setUsageLimit(request.getUsageLimit());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        entity.setMinimumBookingAmount(request.getMinimumBookingAmount());
    }

    private DiscountResponse mapToResponse(Discount entity) {
        return new DiscountResponse(
                entity.getId(),
                entity.getCode(),
                entity.getDiscountType(),
                entity.getValue(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getUsageLimit(),
                entity.getCurrentUsage(),
                entity.getIsActive(),
                entity.getMinimumBookingAmount()
        );
    }
}
