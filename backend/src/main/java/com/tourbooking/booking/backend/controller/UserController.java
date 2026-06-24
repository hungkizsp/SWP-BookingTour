package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved all users")
                .data(userService.getAllUsers())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved user details")
                .data(userService.getUserById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody UserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(userService.createUser(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User updated successfully")
                .data(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User deleted successfully")
                .build();
    }

    @GetMapping("/email")
    public ApiResponse<UserResponse> getUserByEmail(@RequestParam String email) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved user by email")
                .data(userService.getUserByEmail(email))
                .build();
    }

    // UC24
    @PostMapping("/{id}/documents")
    public ApiResponse<String> uploadDocument(
            @PathVariable Long id, 
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Document uploaded successfully")
                .data(userService.uploadDocument(id, file))
                .build();
    }

    // UC26
    @GetMapping("/{id}/loyalty-points")
    public ApiResponse<Integer> getLoyaltyPoints(@PathVariable Long id) {
        return ApiResponse.<Integer>builder()
                .code(HttpStatus.OK.value())
                .message("Loyalty points retrieved")
                .data(userService.getLoyaltyPoints(id))
                .build();
    }
}
