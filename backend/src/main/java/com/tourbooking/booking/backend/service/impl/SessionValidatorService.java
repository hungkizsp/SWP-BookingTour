package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionValidatorService {

    private final UserRepository userRepository;

    public void validateSession(String email, String sessionId) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user = userOpt.orElse(null);
        
        if (user == null || user.getCurrentSessionId() == null
                || !sessionId.equals(user.getCurrentSessionId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại."
            );
        }
    }
}
