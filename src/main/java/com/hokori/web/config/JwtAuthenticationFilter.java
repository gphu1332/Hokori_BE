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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        
        String path = request.getRequestURI();
        logger.debug("🔍 JWT Filter processing request: " + path);
        
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
            logger.debug("Processing JWT token for email: " + email);

            // Validate token
            if (jwtConfig.validateToken(jwtToken, email)) {
                logger.debug("JWT token validated successfully for: " + email);
                
                try {
                    // First, extract roles from JWT token (preferred - avoids database LOB issues)
                    List<String> roles = new ArrayList<>();
                    try {
                        var claims = jwtConfig.extractAllClaims(jwtToken);
                        Object rolesObj = claims.get("roles");
                        
                        if (rolesObj != null) {
                            logger.debug("Found roles in token: " + rolesObj.getClass().getName() + " = " + rolesObj);
                            
                            // Handle different types: List, ArrayList, Object[], String[]
                            if (rolesObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> rolesList = (List<Object>) rolesObj;
                                roles = rolesList.stream()
                                        .map(r -> {
                                            if (r == null) return null;
                                            String roleStr = r.toString().trim();
                                            return roleStr.isEmpty() ? null : roleStr;
                                        })
                                        .filter(r -> r != null && !r.isEmpty())
                                        .collect(Collectors.toList());
                            } else if (rolesObj instanceof Object[]) {
                                Object[] rolesArray = (Object[]) rolesObj;
                                roles = java.util.Arrays.stream(rolesArray)
                                        .map(r -> {
                                            if (r == null) return null;
                                            String roleStr = r.toString().trim();
                                            return roleStr.isEmpty() ? null : roleStr;
                                        })
                                        .filter(r -> r != null && !r.isEmpty())
                                        .collect(Collectors.toList());
                            } else {
                                // Single role as string
                                String roleStr = rolesObj.toString().trim();
                                if (!roleStr.isEmpty()) {
                                    roles = List.of(roleStr);
                                }
                            }
                            
                            if (!roles.isEmpty()) {
                                logger.info("✅ Extracted roles from token: " + roles);
                            } else {
                                logger.warn("⚠️ Roles object found in token but resulted in empty list: " + rolesObj);
                            }
                        } else {
                            logger.warn("⚠️ No 'roles' claim found in JWT token for user: " + email);
                        }
                    } catch (Exception ex) {
                        logger.error("❌ Failed to extract roles from token: " + ex.getMessage());
                        logger.error("   Exception type: " + ex.getClass().getName());
                        ex.printStackTrace();
                    }
                    
                    // Check user exists and is active (simple JPQL query - no LOB fields)
                    // This works for both SQL Server and PostgreSQL
                    boolean userExistsAndActive = false;
                    try {
                        var statusOpt = userRepository.findUserActiveStatusByEmail(email);
                        if (statusOpt.isPresent()) {
                            Object[] status = statusOpt.get();
                            // JPQL query returns: [id, isActive]
                            if (status.length >= 2 && status[1] instanceof Boolean) {
                                userExistsAndActive = Boolean.TRUE.equals(status[1]);
                            } else if (status.length >= 2 && status[1] != null) {
                                // Handle different boolean representations
                                String isActiveStr = status[1].toString().toLowerCase();
                                userExistsAndActive = "true".equals(isActiveStr) || "1".equals(isActiveStr) || "t".equals(isActiveStr);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("❌ Failed to check user active status: " + e.getMessage());
                        logger.error("   Exception type: " + e.getClass().getName());
                        // Continue - we'll proceed with authentication but log warning
                        logger.warn("⚠️ Proceeding with authentication without verifying user active status");
                    }
                    
                    // If user doesn't exist or is not active, reject authentication
                    if (!userExistsAndActive) {
                        logger.warn("⚠️ User " + email + " not found or not active");
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Get roles from token (preferred) or from database (fallback)
                    if (roles.isEmpty()) {
                        // Token doesn't have roles, get from database
                        logger.warn("⚠️ Roles from token are empty, checking database as fallback...");
                        try {
                            Optional<User> userOpt = userRepository.findByEmailWithRole(email);
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                if (user.getRole() != null && user.getRole().getRoleName() != null) {
                                    String roleName = user.getRole().getRoleName().trim().toUpperCase();
                                    roles = List.of(roleName);
                                    logger.info("✅ Got role from database: " + roleName);
                                } else {
                                    logger.error("❌ User " + email + " has no role assigned in database!");
                                }
                            } else {
                                logger.error("❌ User " + email + " not found in database!");
                            }
                        } catch (Exception e) {
                            logger.error("❌ Failed to get role from database: " + e.getMessage());
                            logger.error("   Exception type: " + e.getClass().getName());
                            // Continue without role - user will be authenticated but without authorities
                        }
                    } else {
                        logger.info("✅ Using roles from JWT token for user " + email + ": " + roles);
                    }
                    
                    // Convert roles to authorities (normalize role name to uppercase)
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    try {
                        authorities = roles.stream()
                                .map(role -> {
                                    if (role == null) {
                                        logger.warn("⚠️ Found null role in roles list");
                                        return null;
                                    }
                                    return role.toUpperCase().trim(); // Normalize role name
                                })
                                .filter(role -> role != null && !role.isEmpty()) // Filter out null and empty roles
                                .map(role -> {
                                    // Remove ROLE_ prefix if already present, then add it
                                    String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;
                                    String authority = "ROLE_" + normalizedRole;
                                    logger.debug("   Converting role '" + role + "' to authority '" + authority + "'");
                                    return new SimpleGrantedAuthority(authority);
                                })
                                .filter(auth -> auth != null) // Additional safety check
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        logger.error("❌ Error converting roles to authorities: " + e.getMessage(), e);
                        logger.error("   Roles list: " + roles);
                        e.printStackTrace();
                    }
                    
                    if (authorities.isEmpty()) {
                        logger.error("❌ User " + email + " has NO authorities! User will be authenticated but cannot access role-protected endpoints!");
                        logger.error("   Roles extracted from token: " + roles);
                        logger.error("   This will cause 403 Forbidden errors on role-protected endpoints like /api/teacher/**");
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
                    try {
                        UsernamePasswordAuthenticationToken authToken = 
                                new UsernamePasswordAuthenticationToken(email, null, authorities);
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.info("✅ Authentication SET for user: " + email + " with " + authorities.size() + " authorities");
                        logger.info("   Authorities: " + authorities.stream()
                                .map(a -> a.getAuthority())
                                .collect(Collectors.joining(", ")));
                    } catch (Exception e) {
                        logger.error("❌ CRITICAL: Failed to set authentication for user " + email + ": " + e.getMessage(), e);
                    }
                } catch (Exception e) {
                    logger.error("❌ CRITICAL: Cannot set user authentication for email: " + email, e);
                    logger.error("   Exception type: " + e.getClass().getName());
                    logger.error("   Exception message: " + e.getMessage());
                    if (e.getCause() != null) {
                        logger.error("   Cause: " + e.getCause().getMessage());
                    }
                    // Print stack trace for debugging
                    e.printStackTrace();
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
               path.startsWith("/api/debug/jwt") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs/") ||
               path.equals("/health");
    }
}

