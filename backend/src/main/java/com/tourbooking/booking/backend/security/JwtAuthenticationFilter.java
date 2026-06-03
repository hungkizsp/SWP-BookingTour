package com.tourbooking.booking.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import com.tourbooking.booking.backend.repository.UserRepository;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        // Bỏ qua filter chỉ cho các endpoint auth công cộng (login, register, forgot, reset, verify, events)
        String path = request.getServletPath();
        boolean skip = path.startsWith("/api/v1/payments/") ||
                path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/auth/forgot-password") ||
                path.equals("/api/v1/auth/reset-password") ||
                path.equals("/api/v1/auth/verify") ||
                path.equals("/api/v1/auth/events");
        if (skip) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parseClaims(token);
            String email = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);

            if (email != null && sessionId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Query DB to enforce strict single-session concurrency
                com.tourbooking.booking.backend.model.entity.User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null && user.getCurrentSessionId() != null && user.getCurrentSessionId().equals(sessionId)) {
                    String roleName = claims.get("role", String.class);
                    if (roleName == null) roleName = "CUSTOMER";

                    org.springframework.security.core.authority.SimpleGrantedAuthority authority = 
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleName);
                    
                    UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                            .password(user.getPasswordHash())
                            .authorities(java.util.Collections.singletonList(authority))
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ignored) {
            // Token không hợp lệ hoặc hết hạn -> Không set Authentication, 
            // các endpoint yêu cầu login sẽ tự bị chặn ở SecurityFilterChain
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring("Bearer ".length()).trim();
        }
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        return null;
    }
}
