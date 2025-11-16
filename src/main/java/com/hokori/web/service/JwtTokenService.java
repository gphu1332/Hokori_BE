package com.hokori.web.service;

import com.hokori.web.config.JwtConfig;
import com.hokori.web.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to handle JWT token generation and validation
 * Unified approach for both Firebase and username/password authentication
 */
@Service
public class JwtTokenService {

    @Autowired
    private JwtConfig jwtConfig;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Generate access token for user (works for both Firebase and password auth).
     * 
     * CRITICAL: Roles MUST be included in the token claims.
     * The roles list should contain normalized uppercase role names (e.g., "TEACHER", "ADMIN").
     * 
     * @param user User entity
     * @param loginType "firebase", "password", "refresh", etc.
     * @param roles List of normalized role names (uppercase, no ROLE_ prefix)
     * @return JWT access token string
     */
    public String generateAccessToken(User user, String loginType, List<String> roles) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        
        Map<String, Object> claims = new HashMap<>();
        
        // User identification
        claims.put("userId", user.getId());
        claims.put("sub", user.getEmail()); // Standard JWT subject claim
        
        // CRITICAL: Roles must be stored as a List<String> in the token
        // This ensures JwtAuthenticationFilter can extract them reliably
        // Roles should be normalized (uppercase, no ROLE_ prefix) before calling this method
        if (roles == null || roles.isEmpty()) {
            // Log warning but don't fail - token will be generated without roles
            // User will be authenticated but cannot access role-protected endpoints
            claims.put("roles", List.<String>of());
        } else {
            // Ensure roles are clean (no nulls, trimmed, uppercase)
            List<String> cleanRoles = roles.stream()
                    .filter(r -> r != null && !r.trim().isEmpty())
                    .map(r -> r.trim().toUpperCase())
                    .distinct()
                    .toList();
            claims.put("roles", cleanRoles);
        }
        
        // Authentication metadata
        claims.put("loginType", loginType != null ? loginType : "unknown");
        claims.put("tokenType", "access");
        
        // Add Firebase UID if available (for Firebase users)
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().trim().isEmpty()) {
            claims.put("firebaseUid", user.getFirebaseUid());
        }
        
        return jwtConfig.generateToken(user.getEmail(), claims);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user) {
        return jwtConfig.generateRefreshToken(user.getEmail());
    }

    /**
     * Validate token and return email
     */
    public boolean validateToken(String token, String email) {
        return jwtConfig.validateToken(token, email);
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return jwtConfig.extractUsername(token);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        try {
            var claims = jwtConfig.extractAllClaims(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract login type from token (firebase or password)
     */
    public String extractLoginType(String token) {
        try {
            var claims = jwtConfig.extractAllClaims(token);
            return claims.get("loginType", String.class);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            var claims = jwtConfig.extractAllClaims(token);
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(String token) {
        return jwtConfig.isRefreshToken(token);
    }

    /**
     * Get token expiration time
     */
    public Long getTokenExpiration() {
        return jwtExpiration;
    }
}
