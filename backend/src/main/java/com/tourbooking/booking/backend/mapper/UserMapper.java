package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.model.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        if (user == null) return null;
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setIsActive(user.getIsActive());
        response.setEnabled(Boolean.TRUE.equals(user.getIsActive()));
        return response;
    }

    public static User toEntity(UserRequest request) {
        if (request == null) return null;
        User user = new User();
        updateEntityFromRequest(user, request);
        return user;
    }

    public static void updateEntityFromRequest(User user, UserRequest request) {
        if (request == null || user == null) return;
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        // Password should be handled specifically (encoded) in Service layer
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
    }
}
