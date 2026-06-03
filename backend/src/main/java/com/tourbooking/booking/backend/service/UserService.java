package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.model.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    PagedResponse<UserResponse> getAllUsersPaged(Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    UserResponse getUserByEmail(String email);

    void saveVerificationToken(String email, String token);

    boolean verifyEmail(String token);

    void saveResetPasswordToken(String email, String token);

    boolean resetPassword(String token, String newPassword);

    String rotateSession(String email);

    void clearSession(String email);

    void toggleUserStatus(Long id, boolean isActive);

    long countAllUsers();

    long countOnlineUsers();

    // UC24
    String uploadDocument(Long userId, org.springframework.web.multipart.MultipartFile file);

    // UC26
    Integer getLoyaltyPoints(Long userId);
}
