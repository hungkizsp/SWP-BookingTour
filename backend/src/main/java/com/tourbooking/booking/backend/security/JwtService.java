package com.tourbooking.booking.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tourbooking.booking.backend.model.dto.response.UserResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.exp-minutes:120}") long expMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.expMinutes = expMinutes;
    }

    public String generateToken(UserResponse user, String sessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expMinutes * 60L);
        String activeSessionId = Objects.requireNonNull(sessionId, "sessionId is required");

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "userId", user.getId(),
                        "role", user.getRole() == null ? "CUSTOMER" : user.getRole().name(),
                        "sessionId", activeSessionId
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot init JWT key", e);
        }
    }
}
