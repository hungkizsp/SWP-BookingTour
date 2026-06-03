package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.repository.TokenRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteUnverifiedUsers() {

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(180);
        List<User> unverifiedUsers = userRepository.findByIsActiveFalseAndCreatedAtBefore(cutoff);

        if (!unverifiedUsers.isEmpty()) {
            for (User user : unverifiedUsers) {
                // Xoá token nếu có
                tokenRepository.deleteByEmail(user.getEmail());
            }
            // Xoá user
            userRepository.deleteAll(unverifiedUsers);
            log.info("Xoá {} users chưa xác thực quá 180s.", unverifiedUsers.size());
        }
    }
}
