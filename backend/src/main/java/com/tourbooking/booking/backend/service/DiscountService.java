package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.DiscountRequest;
import com.tourbooking.booking.backend.model.dto.response.DiscountResponse;
import java.math.BigDecimal;
import java.util.List;

public interface DiscountService {
    List<DiscountResponse> getAllDiscounts();
    DiscountResponse getDiscountById(Long id);
    DiscountResponse createDiscount(DiscountRequest request);
    DiscountResponse updateDiscount(Long id, DiscountRequest request);
    void deleteDiscount(Long id);
    DiscountResponse validateCode(String code, BigDecimal amount);
}
