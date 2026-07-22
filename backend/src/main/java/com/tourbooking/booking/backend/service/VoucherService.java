package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.voucher.VoucherAdminRequest;
import com.tourbooking.booking.backend.model.dto.response.VoucherAdminResponse;
import org.springframework.data.domain.Page;

public interface VoucherService {
    Page<VoucherAdminResponse> getAllVouchers(int page, int size, Boolean activeOnly);
    VoucherAdminResponse createVoucher(VoucherAdminRequest request);
    VoucherAdminResponse updateVoucher(Long id, VoucherAdminRequest request);
    void deleteVoucher(Long id);
}
