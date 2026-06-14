package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final ScheduledTaskService scheduledTaskService;

    @PostMapping("/uc46")
    public ApiResponse<Map<String, String>> triggerUC46() {
        scheduledTaskService.autoUpdateSlots();
        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("UC46 - Auto Update Slots executed successfully")
                .data(Map.of("uc", "UC46", "name", "Auto Update Slots", "status", "DONE"))
                .build();
    }

    @PostMapping("/uc47")
    public ApiResponse<Map<String, String>> triggerUC47() {
        scheduledTaskService.autoExpireUnpaidBookings();
        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("UC47 - Auto Cancel Unpaid Bookings executed successfully")
                .data(Map.of("uc", "UC47", "name", "Auto Cancel Unpaid Booking", "status", "DONE"))
                .build();
    }



    @PostMapping("/uc50")
    public ApiResponse<Map<String, String>> triggerUC50() {
        scheduledTaskService.generateMonthlyReport();
        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("UC50 - Generate Monthly Report executed successfully")
                .data(Map.of("uc", "UC50", "name", "Generate Monthly Report", "status", "DONE"))
                .build();
    }

    @PostMapping("/all")
    public ApiResponse<Map<String, String>> triggerAll() {
        scheduledTaskService.autoUpdateSlots();
        scheduledTaskService.autoExpireUnpaidBookings();
        scheduledTaskService.generateMonthlyReport();
        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message("All System Automatic UCs executed successfully")
                .data(Map.of("status", "DONE"))
                .build();
    }
}
