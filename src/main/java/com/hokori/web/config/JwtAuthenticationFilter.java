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
                    
                    // Get user from repository - use simple findByEmail to avoid LOB issues
                    // Only query database if we need to check isActive or get role from DB
                    Optional<User> userOpt = Optional.empty();
                    boolean needDatabaseCheck = roles.isEmpty(); // Only need DB if no roles in token
                    
                    if (needDatabaseCheck) {
                        // Only query DB if token doesn't have roles (fallback case)
                        try {
                            userOpt = userRepository.findByEmailWithRoleForAuth(email);
                            logger.debug("findByEmailWithRoleForAuth result: " + (userOpt.isPresent() ? "found" : "empty"));
                        } catch (Exception e) {
                            logger.warn("⚠️ findByEmailWithRoleForAuth failed, trying findByEmail: " + e.getMessage());
                            userOpt = userRepository.findByEmail(email);
                        }
                    } else {
                        // Token has roles, use native query to check user exists and is active (avoids LOB)
                        try {
                            var basicInfoOpt = userRepository.findUserBasicInfoByEmail(email);
                            if (basicInfoOpt.isPresent()) {
                                Object[] info = basicInfoOpt.get();
                                
                                // Log array length for debugging
                                logger.debug("findUserBasicInfoByEmail returned array length: " + info.length);
                                if (info.length > 0) {
                                    logger.debug("Array types: " + java.util.Arrays.stream(info)
                                        .map(obj -> obj != null ? obj.getClass().getName() : "null")
                                        .collect(java.util.stream.Collectors.joining(", ")));
                                }
                                
                                // Handle nested array case (PostgreSQL sometimes returns Object[] inside Object[])
                                Object[] actualInfo = info;
                                if (info.length == 1 && info[0] instanceof Object[]) {
                                    // Unwrap nested array
                                    actualInfo = (Object[]) info[0];
                                    logger.debug("Unwrapped nested array, new length: " + actualInfo.length);
                                }
                                
                                // Safely extract userId - handle different number types from PostgreSQL
                                Long userId = null;
                                if (actualInfo.length > 0 && actualInfo[0] != null) {
                                    Object userIdObj = actualInfo[0];
                                    // Handle nested array case
                                    if (userIdObj instanceof Object[]) {
                                        Object[] nested = (Object[]) userIdObj;
                                        if (nested.length > 0 && nested[0] instanceof Number) {
                                            userId = ((Number) nested[0]).longValue();
                                        }
                                    } else if (userIdObj instanceof Number) {
                                        userId = ((Number) userIdObj).longValue();
                                    } else {
                                        try {
                                            userId = Long.parseLong(userIdObj.toString());
                                        } catch (NumberFormatException ex) {
                                            logger.warn("⚠️ Could not parse userId: " + userIdObj + " (type: " + userIdObj.getClass().getName() + ")");
                                        }
                                    }
                                }
                                
                                // Safely extract email - check array bounds
                                String userEmail = null;
                                if (actualInfo.length > 1 && actualInfo[1] != null) {
                                    Object emailObj = actualInfo[1];
                                    // Handle nested array case
                                    if (emailObj instanceof Object[]) {
                                        Object[] nested = (Object[]) emailObj;
                                        if (nested.length > 0) {
                                            userEmail = nested[0].toString();
                                        }
                                    } else {
                                        userEmail = emailObj.toString();
                                    }
                                } else {
                                    // Fallback: use email from principal if array doesn't have email
                                    userEmail = email;
                                    logger.debug("⚠️ Email not in query result, using principal email: " + email);
                                }
                                
                                // Handle different boolean types from PostgreSQL (boolean, bit, etc.)
                                Boolean isActive = null;
                                Object isActiveObj = actualInfo.length > 2 ? actualInfo[2] : null;
                                if (isActiveObj != null) {
                                    // Handle nested array case
                                    if (isActiveObj instanceof Object[]) {
                                        Object[] nested = (Object[]) isActiveObj;
                                        if (nested.length > 0) {
                                            isActiveObj = nested[0];
                                        } else {
                                            isActiveObj = null;
                                        }
                                    }
                                    
                                    if (isActiveObj instanceof Boolean) {
                                        isActive = (Boolean) isActiveObj;
                                    } else if (isActiveObj instanceof Number) {
                                        isActive = ((Number) isActiveObj).intValue() != 0;
                                    } else {
                                        // Try to parse as string
                                        String isActiveStr = isActiveObj.toString().toLowerCase();
                                        isActive = "true".equals(isActiveStr) || "1".equals(isActiveStr) || "t".equals(isActiveStr);
                                    }
                                }
                                
                                logger.debug("User basic info found: id=" + userId + ", email=" + userEmail + ", isActive=" + isActive + " (array length: " + actualInfo.length + ")");
                                
                                // Check if user is active
                                if (isActive == null || !isActive) {
                                    logger.warn("⚠️ User " + email + " is NOT active! is_active=" + isActive);
                                    filterChain.doFilter(request, response);
                                    return;
                                }
                                
                                // Create a minimal User object for authentication (we don't need full entity)
                                // Since token has roles, we can proceed with authentication
                                logger.debug("User is active, proceeding with authentication using roles from token");
                            } else {
                                logger.warn("⚠️ User not found for email: " + email);
                                filterChain.doFilter(request, response);
                                return;
                            }
                        } catch (Exception e) {
                            logger.error("❌ Failed to check user basic info: " + e.getMessage(), e);
                            logger.error("   Exception type: " + e.getClass().getName());
                            logger.error("   Stack trace:", e);
                            // Continue without user check - Spring Security will handle
                            // But log warning that we're proceeding without verifying user exists
                            logger.warn("⚠️ Proceeding with authentication without user verification due to error");
                        }
                    }
                    
                    // If we reach here and needDatabaseCheck is true, we have userOpt
                    // If needDatabaseCheck is false, we've already validated user exists and is active
                    if (needDatabaseCheck && userOpt.isEmpty()) {
                        logger.warn("⚠️ User not found for email: " + email);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // For database check path, we need full user entity
                    if (needDatabaseCheck) {
                        var user = userOpt.get();
                        logger.debug("User found: id=" + user.getId() + ", email=" + user.getEmail());
                        
                        // Check if user is active (null-safe check)
                        if (user.getIsActive() == null || !user.getIsActive()) {
                            logger.warn("⚠️ User " + email + " is NOT active! is_active=" + user.getIsActive());
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                    
                    logger.debug("User is active, processing roles...");
                    
                    // Fallback to database roles if token doesn't have roles or roles are empty
                    if (roles.isEmpty() && needDatabaseCheck) {
                        logger.warn("⚠️ Roles from token are empty, checking database as fallback...");
                        try {
                            // Try to get role from database (might fail due to LOB, but worth trying)
                            var user = userOpt.get();
                            if (user.getRole() != null) {
                                String roleName = user.getRole().getRoleName();
                                if (roleName != null && !roleName.trim().isEmpty()) {
                                    // Normalize role name to uppercase for consistency
                                    String normalizedRoleName = roleName.trim().toUpperCase();
                                    roles = List.of(normalizedRoleName);
                                    logger.info("✅ Using database role for user " + email + ": role_id=" + user.getRole().getId() + ", role_name=" + roleName + " (normalized: " + normalizedRoleName + ")");
                                } else {
                                    logger.error("❌ User " + email + " role exists but role_name is null or empty!");
                                }
                            } else {
                                logger.error("❌ User " + email + " has no role assigned in database!");
                            }
                        } catch (Exception e) {
                            logger.error("❌ Could not load role from database (LOB issue): " + e.getMessage());
                            logger.error("   Exception type: " + e.getClass().getName());
                            e.printStackTrace();
                            // Continue without role - user will be authenticated but without authorities
                        }
                    } else if (!roles.isEmpty()) {
                        logger.info("✅ Using roles from JWT token for user " + email + ": " + roles);
                    } else {
                        logger.error("❌ CRITICAL: No roles found in token AND database check was skipped! User: " + email);
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
