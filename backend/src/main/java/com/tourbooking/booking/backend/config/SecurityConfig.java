package com.tourbooking.booking.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tourbooking.booking.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOriginPatterns(java.util.List.of("*"));
                config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(java.util.List.of("*"));
                config.setAllowCredentials(true);
                return config;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Cho phép tất cả các tài nguyên tĩnh
                .requestMatchers(
                    "/", "/error", "/index.html", "/favicon.ico",
                    "/css/**", "/js/**", "/images/**", "/assets/**",
                    "/pages/**",
                    "/user/**", "/admin/**", "/staff/**",
                    "/static/**", "/webjars/**", "/uploads/**"
                ).permitAll()
                // Cho phép các API Auth & Public
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/payments/**").permitAll()
                
                // --- ADMIN & STAFF FEATURES ---
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                .requestMatchers("/api/v1/staff/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                
                // Newsletter: Public subscribe, Staff/Admin manage
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/newsletters").permitAll()
                .requestMatchers("/api/v1/newsletters/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                // Categories & Reviews: Public GET, Staff/Admin manage
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                .requestMatchers("/api/v1/categories/**", "/api/v1/reviews/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                // Bookings: Customer actions
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bookings").authenticated()
                .requestMatchers("/api/v1/bookings/user/**").authenticated()
                .requestMatchers("/api/v1/bookings/apply-voucher").authenticated()
                .requestMatchers("/api/v1/bookings/*/cancel").authenticated()
                .requestMatchers("/api/v1/bookings/*/refund").authenticated()
                .requestMatchers("/api/v1/bookings/*/invoice").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bookings/*").authenticated() // view details
                // Bookings: Staff/Admin manage
                .requestMatchers("/api/v1/bookings/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                // Tours: Public GET, Staff/Admin manage
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/tours/**").permitAll()
                .requestMatchers("/api/v1/tours/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                
                .requestMatchers("/api/v1/chat/**", "/api/v1/ai/**").permitAll()
                
                // Allow /api/v1/demo for automation trigger endpoints
                .requestMatchers("/api/v1/demo/**").permitAll()
                
                // Everything else requires login
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
