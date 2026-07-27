package com.tourbooking.booking.backend.config;

import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DbSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedUser("Quản Trị Viên", "admin@gmail.com", "123456", UserRole.ADMIN);
        seedUser("Khách Hàng", "customer@gmail.com", "123456", UserRole.CUSTOMER);
        seedUser("Hướng Dẫn Viên", "guide@gmail.com", "123456", UserRole.GUIDE);
        seedUser("Nhân Viên", "staff@gmail.com", "123456", UserRole.STAFF);
        System.out.println("✅ Database seeded with 4 default users (password: 123456)");
    }

    private void seedUser(String fullName, String email, String password, UserRole role) {
        try {
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isEmpty()) {
                User user = new User();
                user.setFullName(fullName);
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setRole(role);
                user.setIsActive(true);
                userRepository.save(user);
                System.out.println("Seeded user: " + email);
            }
        } catch (Exception e) {
            System.err.println("Error seeding user " + email + ": " + e.getMessage());
        }
    }
}
