package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.dto.request.voucher.VoucherAdminRequest;
import com.tourbooking.booking.backend.model.dto.response.VoucherAdminResponse;
import com.tourbooking.booking.backend.model.entity.Discount;
import com.tourbooking.booking.backend.model.entity.Tour;
import com.tourbooking.booking.backend.repository.DiscountRepository;
import com.tourbooking.booking.backend.repository.TourRepository;
import com.tourbooking.booking.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final DiscountRepository discountRepository;
    private final TourRepository tourRepository;

    private VoucherAdminResponse mapToResponse(Discount discount) {
        return VoucherAdminResponse.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .discountType(discount.getDiscountType())
                .value(discount.getValue())
                .minimumBookingAmount(discount.getMinimumBookingAmount())
                .maxDiscountAmount(discount.getMaxDiscountAmount())
                .usageLimit(discount.getUsageLimit())
                .currentUsage(discount.getCurrentUsage())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .isActive(discount.getIsActive())
                .applicableTourId(discount.getApplicableTour() != null ? discount.getApplicableTour().getId() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherAdminResponse> getAllVouchers(int page, int size, Boolean activeOnly) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Discount> discounts;
        if (activeOnly != null && activeOnly) {
            discounts = discountRepository.findByIsActiveTrue(pageRequest);
        } else {
            discounts = discountRepository.findAll(pageRequest);
        }
        return discounts.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public VoucherAdminResponse createVoucher(VoucherAdminRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã giảm giá đã tồn tại");
        }
        
        Discount discount = new Discount();
        discount.setCode(request.getCode().toUpperCase());
        discount.setDiscountType(request.getDiscountType());
        discount.setValue(request.getValue());
        discount.setMinimumBookingAmount(request.getMinimumBookingAmount());
        discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        discount.setUsageLimit(request.getUsageLimit());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        discount.setCurrentUsage(0);
        
        if (request.getApplicableTourId() != null) {
            Tour tour = tourRepository.findById(request.getApplicableTourId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
            discount.setApplicableTour(tour);
        }
        
        return mapToResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public VoucherAdminResponse updateVoucher(Long id, VoucherAdminRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy mã giảm giá"));
        
        if (!discount.getCode().equalsIgnoreCase(request.getCode()) && discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã giảm giá đã tồn tại");
        }

        discount.setCode(request.getCode().toUpperCase());
        discount.setDiscountType(request.getDiscountType());
        discount.setValue(request.getValue());
        discount.setMinimumBookingAmount(request.getMinimumBookingAmount());
        discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        discount.setUsageLimit(request.getUsageLimit());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        
        if (request.getIsActive() != null) {
            discount.setIsActive(request.getIsActive());
        }

        if (request.getApplicableTourId() != null) {
            Tour tour = tourRepository.findById(request.getApplicableTourId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
            discount.setApplicableTour(tour);
        } else {
            discount.setApplicableTour(null);
        }

        return mapToResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy mã giảm giá"));
        // Soft delete
        discount.setIsActive(false);
        discountRepository.save(discount);
    }
}
