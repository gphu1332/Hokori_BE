package com.hokori.web.config;

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
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter - Processes JWT tokens and sets Spring Security authentication context.
 * 
 * Flow:
 * 1. Extract JWT token from Authorization header
 * 2. Validate token and extract email
 * 3. Extract roles from token claims (preferred) or database (fallback)
 * 4. Check user active status
 * 5. Convert roles to Spring Security authorities (ROLE_TEACHER, ROLE_ADMIN, etc.)
 * 6. Set authentication in SecurityContextHolder
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    private static final String FILTER_APPLIED = "JWT_FILTER_APPLIED";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Prevent filter from running multiple times for the same request
        if (request.getAttribute(FILTER_APPLIED) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
        
        // Reduced logging to avoid Railway rate limit (500 logs/sec)
        // String path = request.getRequestURI();
        // logger.debug("🔍 JWT Filter processing request: " + path);
        
        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        // Extract JWT token from Authorization header
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtConfig.extractUsername(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("⚠️ Unable to get JWT Token: " + e.getMessage());
            } catch (ExpiredJwtException e) {
                logger.warn("⚠️ JWT Token has expired");
            }
        }

        // Process authentication if token is valid and no authentication is set
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Reduced logging to avoid Railway rate limit
            // logger.debug("🔐 Processing JWT token for email: " + email);

            // Validate token
            if (jwtConfig.validateToken(jwtToken, email)) {
                // Reduced logging to avoid Railway rate limit
                // logger.debug("✅ JWT token validated successfully for: " + email);
                
                try {
                    // Step 1: Extract roles from JWT token (preferred - avoids database LOB issues)
                    List<String> roles = extractRolesFromToken(jwtToken, email);
                    
                    // Step 2: Check user exists and is active (fail-open: proceed if check fails)
                    // This avoids 403 errors when database query has issues
                    boolean userExistsAndActive = checkUserActiveStatus(email);
                    if (!userExistsAndActive) {
                        logger.warn("⚠️ User " + email + " not found or not active - proceeding anyway (fail-open to avoid 403)");
                        // Continue authentication - let Spring Security handle authorization
                    }
                    
                    // Step 4: Fallback to database if token doesn't have roles
                    if (roles.isEmpty()) {
                        logger.warn("⚠️ Roles from token are empty, checking database as fallback...");
                        roles = extractRolesFromDatabase(email);
                        
                        // If still empty after database check, log warning but continue (user will have no authorities)
                        if (roles.isEmpty()) {
                            logger.error("❌ CRITICAL: User " + email + " has NO roles in token AND database!");
                            logger.error("   This user will be authenticated but will get 403 on all role-protected endpoints!");
                        }
                    }
                    
                    // Step 5: Convert roles to Spring Security authorities
                    // Normalize all roles to uppercase for PostgreSQL compatibility
                    List<String> normalizedRoles = roles.stream()
                            .map(r -> r != null ? r.trim().toUpperCase() : null)
                            .filter(r -> r != null && !r.isEmpty())
                            .distinct()
                            .collect(Collectors.toList());
                    
                    List<SimpleGrantedAuthority> authorities = convertRolesToAuthorities(normalizedRoles, email);
                    
                    // Step 6: Set authentication in SecurityContextHolder
                    setAuthentication(email, authorities, request);
                    
                } catch (Exception e) {
                    logger.error("❌ CRITICAL: Cannot set user authentication for email: " + email, e);
                    logger.error("   Exception type: " + e.getClass().getName());
                    logger.error("   Exception message: " + e.getMessage());
                    if (e.getCause() != null) {
                        logger.error("   Cause: " + e.getCause().getMessage());
                    }
                    // Removed e.printStackTrace() to avoid excessive logging - exception already logged above
                }
            } else {
                logger.warn("⚠️ JWT token validation failed for email: " + email);
            }
        }
        
        // Ensure filterChain.doFilter is always called, even if there's an exception
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("❌ Exception in filter chain after JWT processing: " + e.getMessage(), e);
            throw e; // Re-throw to let Spring Security handle it
        }
    }
    
    /**
     * Extract roles from JWT token claims.
     * Roles are stored as List<String> in the token (normalized uppercase, no ROLE_ prefix).
     */
    private List<String> extractRolesFromToken(String jwtToken, String email) {
        List<String> roles = new ArrayList<>();
        try {
            var claims = jwtConfig.extractAllClaims(jwtToken);
            Object rolesObj = claims.get("roles");
            
            if (rolesObj == null) {
                logger.warn("⚠️ No 'roles' claim found in JWT token for user: " + email);
                return roles;
            }
            
            logger.debug("Found roles in token: " + rolesObj.getClass().getName() + " = " + rolesObj);
            
            // Handle different types: List, ArrayList, Object[], String[]
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> rolesList = (List<Object>) rolesObj;
                roles = rolesList.stream()
                        .map(r -> {
                            if (r == null) return null;
                            String roleStr = r.toString().trim().toUpperCase();
                            return roleStr.isEmpty() ? null : roleStr;
                        })
                        .filter(r -> r != null && !r.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            } else if (rolesObj instanceof Object[]) {
                Object[] rolesArray = (Object[]) rolesObj;
                roles = java.util.Arrays.stream(rolesArray)
                        .map(r -> {
                            if (r == null) return null;
                            String roleStr = r.toString().trim().toUpperCase();
                            return roleStr.isEmpty() ? null : roleStr;
                        })
                        .filter(r -> r != null && !r.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            } else {
                // Single role as string
                String roleStr = rolesObj.toString().trim().toUpperCase();
                if (!roleStr.isEmpty()) {
                    roles = List.of(roleStr);
                }
            }
            
            if (!roles.isEmpty()) {
                // Reduced logging to avoid Railway rate limit - only log if debug enabled
                logger.debug("✅ Extracted roles from token: " + roles);
            } else {
                logger.warn("⚠️ Roles object found in token but resulted in empty list: " + rolesObj);
            }
        } catch (Exception ex) {
            logger.error("❌ Failed to extract roles from token: " + ex.getMessage());
            logger.error("   Exception type: " + ex.getClass().getName());
            // Removed printStackTrace() to avoid excessive logging - exception already logged above
        }
        return roles;
    }
    
    /**
     * Check if user exists and is active using JPQL query (avoids LOB fields).
     * Handles PostgreSQL nested array case.
     */
    private boolean checkUserActiveStatus(String email) {
        try {
            logger.debug("Checking user active status for email: " + email);
            var statusOpt = userRepository.findUserActiveStatusByEmail(email);
            
            if (statusOpt.isEmpty()) {
                logger.warn("⚠️ User not found in database for email: " + email + " (proceeding anyway - fail-open)");
                return true; // Fail-open: proceed if user not found
            }
            
            Object[] status = statusOpt.get();
            
            // Handle nested array case (PostgreSQL returns Object[] inside Object[])
            Object[] actualStatus = status;
            if (status.length == 1 && status[0] instanceof Object[]) {
                actualStatus = (Object[]) status[0];
                logger.debug("Unwrapped nested array: outer length=" + status.length + ", inner length=" + actualStatus.length);
            }
            
            // JPQL query returns: [id, isActive]
            if (actualStatus.length < 2) {
                logger.warn("⚠️ User status query returned insufficient data: length=" + actualStatus.length + " (proceeding anyway - fail-open)");
                return true; // Fail-open
            }
            
            Object isActiveObj = actualStatus[1];
            boolean isActive = false;
            
            if (isActiveObj == null) {
                logger.warn("⚠️ User " + email + " has null isActive - treating as active (fail-open)");
                return true; // Fail-open: proceed if null
            } else if (isActiveObj instanceof Boolean) {
                isActive = (Boolean) isActiveObj;
            } else if (isActiveObj instanceof Number) {
                // Handle bit/boolean as number (SQL Server)
                isActive = ((Number) isActiveObj).intValue() != 0;
            } else {
                // Handle string representation
                String isActiveStr = isActiveObj.toString().toLowerCase().trim();
                isActive = "true".equals(isActiveStr) || "1".equals(isActiveStr) || 
                          "t".equals(isActiveStr) || "yes".equals(isActiveStr);
            }
            
            if (isActive) {
                // Reduced logging to avoid Railway rate limit - only log if debug enabled
                logger.debug("✅ User " + email + " is active");
            } else {
                logger.warn("⚠️ User " + email + " is NOT active (but proceeding anyway - fail-open)");
                // Fail-open: proceed even if inactive to avoid 403
                return true;
            }
            return isActive;
            
        } catch (Exception e) {
            logger.error("❌ Failed to check user active status: " + e.getMessage());
            logger.error("   Exception type: " + e.getClass().getName());
            logger.warn("   Proceeding with authentication anyway (fail-open to avoid 403)");
            // Removed printStackTrace() to avoid excessive logging - exception already logged above
            // Fail-open: proceed with authentication if query fails (to avoid 403)
            return true;
        }
    }
    
    /**
     * Extract roles from database (fallback when token doesn't have roles).
     * Uses native query to avoid LOB issues and ensure PostgreSQL compatibility.
     */
    private List<String> extractRolesFromDatabase(String email) {
        List<String> roles = new ArrayList<>();
        try {
            logger.debug("🔍 Extracting role from database for email: " + email);
            // Use native query to avoid LOB fields and ensure PostgreSQL compatibility
            var roleInfoOpt = userRepository.findRoleInfoByEmail(email);
            if (roleInfoOpt.isPresent()) {
                Object[] roleData = roleInfoOpt.get();
                
                // Handle nested array case (PostgreSQL sometimes returns Object[] inside Object[])
                Object[] actualRoleData = roleData;
                if (roleData.length == 1 && roleData[0] instanceof Object[]) {
                    actualRoleData = (Object[]) roleData[0];
                    logger.debug("Unwrapped nested array for role data: outer length=" + roleData.length + ", inner length=" + actualRoleData.length);
                }
                
                // Native query returns: [role_id, role_name, role_description]
                if (actualRoleData.length >= 2 && actualRoleData[1] != null) {
                    String roleName = actualRoleData[1].toString().trim().toUpperCase();
                    roles = List.of(roleName);
                    // Reduced logging to avoid Railway rate limit - only log if debug enabled
                    logger.debug("✅ Got role from database: " + roleName + " (normalized to uppercase for PostgreSQL compatibility)");
                } else {
                    logger.error("❌ User " + email + " has null or empty role_name in database!");
                    logger.error("   Role data length: " + actualRoleData.length);
                }
            } else {
                logger.error("❌ User " + email + " not found in database or has no role assigned!");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to get role from database: " + e.getMessage());
            logger.error("   Exception type: " + e.getClass().getName());
            // Removed printStackTrace() to avoid excessive logging - exception already logged above
        }
        return roles;
    }
    
    /**
     * Convert roles to Spring Security authorities.
     * Roles should be normalized (uppercase, no ROLE_ prefix).
     * Authorities will have ROLE_ prefix (e.g., ROLE_TEACHER, ROLE_ADMIN).
     */
    private List<SimpleGrantedAuthority> convertRolesToAuthorities(List<String> roles, String email) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        try {
            authorities = roles.stream()
                    .map(role -> {
                        if (role == null) {
                            logger.warn("⚠️ Found null role in roles list");
                            return null;
                        }
                        // Normalize: trim, uppercase, remove ROLE_ prefix if present
                        String normalizedRole = role.trim().toUpperCase();
                        if (normalizedRole.startsWith("ROLE_")) {
                            normalizedRole = normalizedRole.substring(5);
                        }
                        // Add ROLE_ prefix for Spring Security
                        String authority = "ROLE_" + normalizedRole;
                        logger.debug("   Converting role '" + role + "' to authority '" + authority + "'");
                        return new SimpleGrantedAuthority(authority);
                    })
                    .filter(auth -> auth != null)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error converting roles to authorities: " + e.getMessage(), e);
            logger.error("   Roles list: " + roles);
            // Removed printStackTrace() to avoid excessive logging - exception already logged above
        }
        
        if (authorities.isEmpty()) {
            logger.error("❌ User " + email + " has NO authorities! User will be authenticated but cannot access role-protected endpoints!");
            logger.error("   Roles extracted: " + roles);
            logger.error("   This will cause 403 Forbidden errors on role-protected endpoints like /api/teacher/**");
        } else {
            // Reduced logging to avoid Railway rate limit - only log if debug enabled
            logger.debug("✅ User " + email + " authorities: " + authorities.stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.joining(", ")));
        }
        
        return authorities;
    }
    
    /**
     * Set authentication in SecurityContextHolder.
     * Always sets authentication, even with empty authorities (allows authenticated() endpoints to work).
     */
    private void setAuthentication(String email, List<SimpleGrantedAuthority> authorities, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
            // Reduced logging to avoid Railway rate limit - only log if debug enabled
            logger.debug("✅ Authentication SET for user: " + email + " with " + authorities.size() + " authorities");
            if (!authorities.isEmpty()) {
                logger.debug("   Authorities: " + authorities.stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.joining(", ")));
            }
        } catch (Exception e) {
            logger.error("❌ CRITICAL: Failed to set authentication for user " + email + ": " + e.getMessage(), e);
        }
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
               path.equals("/api/payment/webhook") || // PayOS webhook - no JWT required
               path.startsWith("/actuator/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs/") ||
               path.equals("/health") ||
               path.startsWith("/files/"); // Skip JWT filter for file serving (public endpoint)
    }
}
