package com.hokori.web.config;

import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtConfig.extractUsername(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired");
            }
        }

        // Once we get the token validate it.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validate token
            if (jwtConfig.validateToken(jwtToken, email)) {
                
                try {
                    // Get user from repository WITH role (using fetch join to avoid lazy loading issues)
                    var userOpt = userRepository.findByEmailWithRole(email);
                    if (userOpt.isEmpty()) {
                        // Fallback to regular findByEmail if findByEmailWithRole not available
                        userOpt = userRepository.findByEmail(email);
                    }
                    
                    if (userOpt.isPresent()) {
                        var user = userOpt.get();
                        
                        // Check if user is active (null-safe check)
                        if (user.getIsActive() != null && user.getIsActive()) {
                            // Extract roles from JWT claims (fallback to database)
                            List<String> roles = List.of();
                            try {
                                var claims = jwtConfig.extractAllClaims(jwtToken);
                                @SuppressWarnings("unchecked")
                                var rolesFromToken = (List<String>) claims.get("roles");
                                if (rolesFromToken != null && !rolesFromToken.isEmpty()) {
                                    roles = rolesFromToken;
                                }
                            } catch (Exception ex) {
                                logger.debug("Failed to extract roles from token, will use database roles: " + ex.getMessage());
                            }
                            
                            // Fallback to database roles if token doesn't have roles or roles are empty
                            if (roles.isEmpty()) {
                                if (user.getRole() != null && user.getRole().getRoleName() != null) {
                                    roles = List.of(user.getRole().getRoleName());
                                    logger.info("✅ Using database role for user " + email + ": " + user.getRole().getRoleName());
                                } else {
                                    logger.warn("⚠️ User " + email + " has no role assigned! role_id=" + (user.getRole() != null ? user.getRole().getId() : "null"));
                                }
                            } else {
                                logger.info("✅ Using roles from JWT token for user " + email + ": " + roles);
                            }
                            
                            // Convert roles to authorities (normalize role name to uppercase)
                            var authorities = roles.stream()
                                    .map(role -> role.toUpperCase().trim()) // Normalize role name
                                    .filter(role -> !role.isEmpty()) // Filter out empty roles
                                    .map(role -> {
                                        // Remove ROLE_ prefix if already present, then add it
                                        String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;
                                        return new SimpleGrantedAuthority("ROLE_" + normalizedRole);
                                    })
                                    .collect(Collectors.toList());
                            
                            if (authorities.isEmpty()) {
                                logger.error("❌ User " + email + " has NO authorities! User will be authenticated but cannot access role-protected endpoints!");
                                logger.error("   User role_id: " + (user.getRole() != null ? user.getRole().getId() : "null"));
                                logger.error("   User role_name: " + (user.getRole() != null ? user.getRole().getRoleName() : "null"));
                                // Still set authentication with empty authorities - allows authenticated() endpoints to work
                                // But role-protected endpoints (hasRole) will fail
                            } else {
                                logger.info("✅ User " + email + " authorities: " + authorities.stream()
                                        .map(a -> a.getAuthority())
                                        .collect(Collectors.joining(", ")));
                            }

                            // IMPORTANT: Always set authentication, even with empty authorities
                            // This allows endpoints with .authenticated() to work
                            // But endpoints with hasRole() will fail if authorities are empty
                            UsernamePasswordAuthenticationToken authToken = 
                                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                            
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            logger.debug("Authentication set for user: " + email + " with " + authorities.size() + " authorities");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Cannot set user authentication: {}", e);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filter for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/ai/") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/hello") ||
               path.startsWith("/api/echo") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs/") ||
               path.equals("/health");
    }
}
