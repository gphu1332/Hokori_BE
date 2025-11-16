package com.hokori.web.controller;

import com.hokori.web.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "System", description = "System health and utility endpoints")
@CrossOrigin(origins = "*")
public class HokoriController {

    @Autowired
    private JwtConfig jwtConfig;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the API is running")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Hokori API is running!");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/hello")
    @Operation(summary = "Hello endpoint", description = "Greeting endpoint with optional name parameter")
    public Map<String, String> hello(
            @Parameter(description = "Name to greet") @RequestParam(defaultValue = "World") String name) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello " + name + "!");
        response.put("greeting", "Welcome to Hokori API");
        return response;
    }

    @PostMapping("/echo")
    @Operation(summary = "Echo endpoint", description = "Echo back the received data")
    public Map<String, Object> echo(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", data);
        response.put("echoed_at", LocalDateTime.now());
        response.put("status", "success");
        return response;
    }

    @GetMapping("/debug/jwt")
    @Operation(summary = "Debug JWT token", description = "Public endpoint to parse and debug JWT token (no auth required)")
    public Map<String, Object> debugJwt(
            @Parameter(description = "JWT token (without Bearer prefix)") @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract token from header or query param
            String jwtToken = null;
            if (token != null && !token.isEmpty()) {
                jwtToken = token;
            } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }
            
            if (jwtToken == null || jwtToken.isEmpty()) {
                response.put("error", "No token provided. Use ?token=YOUR_TOKEN or Authorization header");
                response.put("usage", "GET /api/debug/jwt?token=YOUR_JWT_TOKEN");
                return response;
            }
            
            // Parse token
            Claims claims = jwtConfig.extractAllClaims(jwtToken);
            String email = jwtConfig.extractUsername(jwtToken);
            
            response.put("success", true);
            response.put("email", email);
            response.put("userId", claims.get("userId"));
            response.put("loginType", claims.get("loginType"));
            response.put("tokenType", claims.get("tokenType"));
            response.put("firebaseUid", claims.get("firebaseUid"));
            response.put("issuedAt", claims.getIssuedAt());
            response.put("expiration", claims.getExpiration());
            
            // Extract roles with detailed info
            Object rolesObj = claims.get("roles");
            response.put("rolesRaw", rolesObj);
            response.put("rolesType", rolesObj != null ? rolesObj.getClass().getName() : "null");
            
            List<String> roles = new ArrayList<>();
            if (rolesObj != null) {
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rolesList = (List<Object>) rolesObj;
                    roles = rolesList.stream()
                            .map(r -> r != null ? r.toString() : null)
                            .filter(r -> r != null && !r.isEmpty())
                            .collect(Collectors.toList());
                } else if (rolesObj instanceof Object[]) {
                    Object[] rolesArray = (Object[]) rolesObj;
                    roles = Arrays.stream(rolesArray)
                            .map(r -> r != null ? r.toString() : null)
                            .filter(r -> r != null && !r.isEmpty())
                            .collect(Collectors.toList());
                } else {
                    roles = List.of(rolesObj.toString());
                }
            }
            
            response.put("roles", roles);
            response.put("rolesCount", roles.size());
            
            // Convert to authorities format
            List<String> authorities = roles.stream()
                    .map(role -> role.toUpperCase().trim())
                    .filter(role -> !role.isEmpty())
                    .map(role -> {
                        String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;
                        return "ROLE_" + normalizedRole;
                    })
                    .collect(Collectors.toList());
            
            response.put("authorities", authorities);
            response.put("authoritiesCount", authorities.size());
            
            // All claims
            Map<String, Object> allClaims = new HashMap<>();
            claims.forEach((key, value) -> allClaims.put(key, value));
            response.put("allClaims", allClaims);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getMessage());
            }
        }
        
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}