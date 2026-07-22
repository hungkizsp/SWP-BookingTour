package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.voucher.VoucherAdminRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.VoucherAdminResponse;
import com.tourbooking.booking.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ApiResponse<Page<VoucherAdminResponse>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean activeOnly) {
        return ApiResponse.<Page<VoucherAdminResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách mã giảm giá thành công")
                .data(voucherService.getAllVouchers(page, size, activeOnly))
                .build();
    }

    @PostMapping
    public ApiResponse<VoucherAdminResponse> createVoucher(@RequestBody VoucherAdminRequest request) {
        return ApiResponse.<VoucherAdminResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tạo mã giảm giá thành công")
                .data(voucherService.createVoucher(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherAdminResponse> updateVoucher(
            @PathVariable Long id,
            @RequestBody VoucherAdminRequest request) {
        return ApiResponse.<VoucherAdminResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật mã giảm giá thành công")
                .data(voucherService.updateVoucher(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa mã giảm giá thành công")
                .build();
    }
}
