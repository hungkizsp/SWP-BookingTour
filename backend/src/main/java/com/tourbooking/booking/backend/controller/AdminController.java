package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.TourRequest;
import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.FinancialReportResponse;
import com.tourbooking.booking.backend.model.dto.response.TourResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.service.BookingService;
import com.tourbooking.booking.backend.service.FileService;
import com.tourbooking.booking.backend.service.TourService;
import com.tourbooking.booking.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final TourService tourService;
    private final UserService userService;
    private final BookingService bookingService;
    private final FileService fileService;

    @Autowired
    public AdminController(TourService tourService, UserService userService, BookingService bookingService, FileService fileService) {
        this.tourService = tourService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.fileService = fileService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    // --- TOUR MANAGEMENT ---

    @PostMapping("/tours/upload")
    public ApiResponse<List<String>> uploadTourImages(@RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.<List<String>>builder()
                .code(HttpStatus.OK.value())
                .message("Images uploaded successfully")
                .data(fileService.uploadImages(files))
                .build();
    }

    @PostMapping("/tours")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TourResponse> createTour(@RequestBody TourRequest request) {
        return ApiResponse.<TourResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tour created successfully")
                .data(tourService.createTour(request))
                .build();
    }

    @PutMapping("/tours/{id}")
    public ApiResponse<TourResponse> updateTour(@PathVariable Long id, @RequestBody TourRequest request) {
        return ApiResponse.<TourResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tour updated successfully")
                .data(tourService.updateTour(id, request))
                .build();
    }

    @DeleteMapping("/tours/{id}")
    public ApiResponse<Void> deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Tour deleted successfully")
                .build();
    }

    // --- USER MANAGEMENT ---

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@RequestBody UserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(userService.createUser(request))
                .build();
    }

    @GetMapping("/users")
    public ApiResponse<com.tourbooking.booking.backend.model.dto.response.PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<com.tourbooking.booking.backend.model.dto.response.PagedResponse<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved users (page " + page + ")")
                .data(userService.getAllUsersPaged(pageable))
                .build();
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved user details")
                .data(userService.getUserById(id))
                .build();
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User updated successfully")
                .data(userService.updateUser(id, request))
                .build();
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> status) {
        boolean isActive = status.getOrDefault("isActive", true);
        userService.toggleUserStatus(id, isActive);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User status updated successfully")
                .build();
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User deleted successfully")
                .build();
    }

    @GetMapping("/financial-report")
    public ApiResponse<List<FinancialReportResponse>> getFinancialReport(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(defaultValue = "all") String status) {
        return ApiResponse.<List<FinancialReportResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Financial report generated successfully")
                .data(bookingService.getFinancialReport(start, end, type, status))
                .build();
    }

    // --- DASHBOARD STATS ---

    @GetMapping("/users/count")
    public ApiResponse<Map<String, Long>> getUserCount() {
        Map<String, Long> data = new HashMap<>();
        data.put("total", userService.countAllUsers());
        data.put("online", userService.countOnlineUsers());
        return ApiResponse.<Map<String, Long>>builder()
                .code(HttpStatus.OK.value())
                .message("User count retrieved")
                .data(data)
                .build();
    }

    @GetMapping("/revenue/month")
    public ApiResponse<Map<String, Object>> getMonthlyRevenue() {
        BigDecimal revenue = bookingService.getMonthlyRevenue();
        Map<String, Object> data = new HashMap<>();
        data.put("revenue", revenue);
        data.put("month", java.time.YearMonth.now().toString());
        return ApiResponse.<Map<String, Object>>builder()
                .code(HttpStatus.OK.value())
                .message("Monthly revenue retrieved")
                .data(data)
                .build();
    }

    @GetMapping("/bookings/active")
    public ApiResponse<Map<String, Long>> getActiveBookings() {
        Map<String, Long> data = new HashMap<>();
        data.put("count", bookingService.countActiveBookings());
        return ApiResponse.<Map<String, Long>>builder()
                .code(HttpStatus.OK.value())
                .message("Active bookings count retrieved")
                .data(data)
                .build();
    }

    @PostMapping("/generate-test-data")
    public ApiResponse<String> generateTestData() {
        bookingService.generateTestData();
        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Test data generated successfully")
                .data("5 sample successful bookings created.")
                .build();
    }
}
