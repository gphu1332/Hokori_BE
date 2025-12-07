package com.hokori.web.config;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize and @PostAuthorize
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // OPTIONS requests must be first (for CORS preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // ============================================
                // PUBLIC ENDPOINTS - No authentication required
                // IMPORTANT: These must come BEFORE role-based and authenticated endpoints
                // ============================================
                .requestMatchers("/api/auth/**").permitAll()
                // AI endpoints: packages require auth, but some AI services might be public
                // /api/ai/packages/** requires authentication (handled by @PreAuthorize)
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/hello").permitAll()
                .requestMatchers("/api/echo").permitAll()
                .requestMatchers("/api/debug/jwt").permitAll()
                .requestMatchers("/api/courses/**").permitAll() // Public marketplace - published courses only
                .requestMatchers("/api/courses-public/**").permitAll() // Public course comments
                .requestMatchers("/api/payment/webhook").permitAll() // PayOS webhook - public access required
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/files/**").permitAll() // Public file serving endpoint

                // 👇 Public marketplace courses (chỉ GET)
                .requestMatchers(HttpMethod.GET,
                    "/api/courses",
                    "/api/courses/*/tree"
                ).permitAll()

                // 👇 Public policies (chỉ đọc)
                .requestMatchers("/api/public/policies/**").permitAll()

                // ============================================
                // ROLE-BASED ENDPOINTS
                // ============================================
                // NOTE: Spring Security requires string literals here (cannot use constants).
                // Role names reference: RoleConstants.LEARNER, RoleConstants.TEACHER, RoleConstants.STAFF, RoleConstants.ADMIN
                
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // RoleConstants.ADMIN
                
                // Moderator-only endpoints
                .requestMatchers("/api/moderator/**").hasRole("MODERATOR") // RoleConstants.MODERATOR
                
                // Staff and Admin endpoints
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN") // RoleConstants.STAFF, RoleConstants.ADMIN
                
                // Teacher, Staff, Admin, and Moderator endpoints
                // Note: Individual methods may have stricter @PreAuthorize annotations
                .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "STAFF", "ADMIN", "MODERATOR") // RoleConstants.TEACHER, RoleConstants.STAFF, RoleConstants.ADMIN, RoleConstants.MODERATOR
                
                // Learner-only endpoints
                .requestMatchers("/api/learner/**").hasRole("LEARNER") // RoleConstants.LEARNER
                
                // ============================================
                // AUTHENTICATED ENDPOINTS - Any authenticated user
                // ============================================
                
                // User profile endpoints - authenticated users only
                .requestMatchers("/api/profile/**").authenticated()
                
                // Cart endpoints - authenticated users only
                .requestMatchers("/api/cart/**").authenticated()
                
                // AI Package endpoints - authenticated users only (handled by @PreAuthorize in controller)
                .requestMatchers("/api/ai/packages/**").authenticated()
                
                // Asset endpoints - authenticated users only (typically TEACHER/ADMIN but checked in service)
                .requestMatchers("/api/assets/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}