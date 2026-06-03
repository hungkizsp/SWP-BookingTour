package com.tourbooking.booking.backend.model.dto.request;

import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import lombok.Data;

@Data
public class UserRequest {
    private String fullName;
    private String email;
    private String password;
    private UserRole role;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private Boolean isActive;
}
