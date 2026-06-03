package com.tourbooking.booking.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourbooking.booking.backend.mapper.UserMapper;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.GoogleAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Verifies Google ID tokens via Google's tokeninfo endpoint,
 * then finds or creates the corresponding local user.
 */
@Service
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String expectedClientId;

    public GoogleAuthServiceImpl(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UserResponse authenticateGoogleUser(String idToken) {
        // 1. Verify the ID token with Google
        JsonNode tokenInfo = verifyIdToken(idToken);

        String email = tokenInfo.path("email").asText(null);
        String name = tokenInfo.path("name").asText(null);
        String picture = tokenInfo.path("picture").asText(null);
        boolean emailVerified = tokenInfo.path("email_verified").asText("false").equals("true");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google token does not contain email");
        }

        if (!emailVerified) {
            throw new RuntimeException("Google email is not verified");
        }

        // 2. Validate audience matches our client ID
        String aud = tokenInfo.path("aud").asText("");
        if (expectedClientId != null && !expectedClientId.isBlank() && !expectedClientId.equals(aud)) {
            log.warn("Google token audience mismatch: expected={}, got={}", expectedClientId, aud);
            throw new RuntimeException("Invalid Google token audience");
        }

        // 3. Find or create user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Create new user from Google profile
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : email.split("@")[0]);
            user.setAvatarUrl(picture);
            user.setRole(UserRole.CUSTOMER);
            user.setIsActive(true); // Google-verified emails are auto-activated
            user.setPasswordHash(""); // No password for Google users
            user = userRepository.save(user);
            log.info("Created new user from Google login: {}", email);
        } else {
            // Update avatar if not set
            if (user.getAvatarUrl() == null && picture != null) {
                user.setAvatarUrl(picture);
                user = userRepository.save(user);
            }
        }

        return UserMapper.toResponse(user);
    }

    private JsonNode verifyIdToken(String idToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKENINFO_URL + idToken))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Google token verification failed: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("Invalid Google ID token");
            }

            return objectMapper.readTree(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying Google ID token", e);
            throw new RuntimeException("Could not verify Google ID token", e);
        }
    }
}
