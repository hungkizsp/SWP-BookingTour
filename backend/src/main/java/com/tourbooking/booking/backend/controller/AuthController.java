package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.dto.request.AuthRequest;
import com.tourbooking.booking.backend.model.dto.request.GoogleLoginRequest;
import com.tourbooking.booking.backend.model.dto.request.ForgotPasswordRequest;
import com.tourbooking.booking.backend.model.dto.request.UserRequest;
import com.tourbooking.booking.backend.model.dto.response.ApiResponse;
import com.tourbooking.booking.backend.model.dto.response.AuthResponse;
import com.tourbooking.booking.backend.model.dto.response.UserResponse;
import com.tourbooking.booking.backend.security.JwtService;
import com.tourbooking.booking.backend.service.AuthSessionNotificationService;
import com.tourbooking.booking.backend.service.GoogleAuthService;
import com.tourbooking.booking.backend.service.MailService;
import com.tourbooking.booking.backend.service.RateLimiterService;
import com.tourbooking.booking.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MailService mailService;
    private final RateLimiterService rateLimiterService;
    private final AuthSessionNotificationService authSessionNotificationService;
    private final JwtService jwtService;
    private final GoogleAuthService googleAuthService;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            MailService mailService,
            RateLimiterService rateLimiterService,
            AuthSessionNotificationService authSessionNotificationService,
            JwtService jwtService,
            GoogleAuthService googleAuthService
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.mailService = mailService;
        this.rateLimiterService = rateLimiterService;
        this.authSessionNotificationService = authSessionNotificationService;
        this.jwtService = jwtService;
        this.googleAuthService = googleAuthService;
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserResponse user = userService.getUserByEmail(request.getEmail());
        if (user.getIsActive() == null || !user.getIsActive()) {
            return ApiResponse.<AuthResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Account is not active")
                    .data(null)
                    .build();
        }

        // `enabled` comes from UserResponse mapping (primitive boolean)
        if (!user.isEnabled()) {
            return ApiResponse.<AuthResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Account not verified")
                    .data(null)
                    .build();
        }

        String sessionId = userService.rotateSession(user.getEmail());
        AuthResponse authResponse = AuthResponse.builder()
                .token(jwtService.generateToken(user, sessionId))
                .user(user)
                .build();

        /* 
        authSessionNotificationService.notifyOthers(
                user.getEmail(),
                sessionId,
                "Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại."
        );
        */

        return ApiResponse.<AuthResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Login successful")
                .data(authResponse)
                .build();
    }

    // ================= ME =================
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ApiResponse.<UserResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized")
                    .data(null)
                    .build();
        }

        UserResponse user = userService.getUserByEmail(principal.getName());
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Current user")
                .data(user)
                .build();
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRequest request) {
        if (!rateLimiterService.tryConsume(request.getEmail())) {
            return ApiResponse.<UserResponse>builder()
                    .code(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message("Too many registration requests. Please try again later.")
                    .data(null)
                    .build();
        }

        UserResponse user = userService.createUser(request);

        try {
            String token = UUID.randomUUID().toString();
            userService.saveVerificationToken(user.getEmail(), token);
            mailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception e) {
            // Không làm gãy luồng đăng ký; email chỉ là bước bổ trợ
            log.warn("Register verification email failed: {}", e.getMessage());
        }

        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Register successful. Please check your email to verify.")
                .data(user)
                .build();
    }

    // ================= VERIFY EMAIL =================
    @GetMapping("/verify")
    public ApiResponse<String> verifyEmail(@RequestParam String token) {
        boolean isValid = userService.verifyEmail(token);

        if (!isValid) {
            return ApiResponse.<String>builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid or expired token")
                    .data(null)
                    .build();
        }

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Email verified successfully")
                .data(null)
                .build();
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String token = UUID.randomUUID().toString();
            userService.saveResetPasswordToken(request.getEmail(), token);

            String baseUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                    : publicBaseUrl;
            String resetLink = baseUrl + "/pages/auth/reset-password.html?token=" + token;
            mailService.sendPasswordResetEmail(request.getEmail(), resetLink);
        } catch (Exception e) {
            log.warn("Forgot-password email failed: {}", e.getMessage());
        }

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("If email exists, reset link has been sent")
                .data(null)
                .build();
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        boolean success = userService.resetPassword(token, newPassword);

        if (!success) {
            return ApiResponse.<String>builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid or expired token")
                    .data(null)
                    .build();
        }

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Password reset successful")
                .data(null)
                .build();
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ApiResponse<String> logout(Principal principal) {
        if (principal != null && principal.getName() != null) {
            userService.clearSession(principal.getName());
            authSessionNotificationService.notifySessionInvalidated(
                    principal.getName(),
                    "Phiên đăng nhập đã bị đăng xuất. Vui lòng đăng nhập lại."
            );
        }

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Logout successful")
                .data(null)
                .build();
    }

    // ================= GOOGLE LOGIN =================
    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            UserResponse user = googleAuthService.authenticateGoogleUser(request.getIdToken());

            if (user.getIsActive() == null || !user.getIsActive()) {
                return ApiResponse.<AuthResponse>builder()
                        .code(HttpStatus.UNAUTHORIZED.value())
                        .message("Account is not active")
                        .data(null)
                        .build();
            }

            String sessionId = userService.rotateSession(user.getEmail());
            AuthResponse authResponse = AuthResponse.builder()
                    .token(jwtService.generateToken(user, sessionId))
                    .user(user)
                    .build();

            /* 
            authSessionNotificationService.notifyOthers(
                    user.getEmail(),
                    sessionId,
                    "Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại."
            );
            */

            return ApiResponse.<AuthResponse>builder()
                    .code(HttpStatus.OK.value())
                    .message("Google login successful")
                    .data(authResponse)
                    .build();
        } catch (Exception e) {
            log.error("Google login failed", e);
            return ApiResponse.<AuthResponse>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage() != null ? e.getMessage() : "Google login failed")
                    .data(null)
                    .build();
        }
    }

    /* 
    // SSE: DISABLED to ensure absolute stability during review
    @GetMapping("/events")
    public SseEmitter events(@RequestParam String token) {
        return authSessionNotificationService.subscribe(token);
    }
    */
}

