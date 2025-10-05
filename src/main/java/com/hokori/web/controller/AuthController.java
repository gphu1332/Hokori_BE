package com.hokori.web.controller;

import com.hokori.web.dto.AuthResponse;
import com.hokori.web.dto.LoginRequest;
import com.hokori.web.dto.RegisterRequest;
import com.hokori.web.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login/firebase")
    @Operation(summary = "Login with Firebase token", description = "Authenticate user with Firebase ID token")
    public ResponseEntity<?> loginWithFirebase(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            if (idToken == null || idToken.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Firebase ID token is required");
                error.put("status", "error");
                error.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.badRequest().body(error);
            }
            
            AuthResponse response = authService.authenticateUser(idToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Firebase authentication failed: " + e.getMessage());
            error.put("status", "error");
            error.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/password", description = "Authenticate user with username/email and password")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.authenticateWithUsernamePassword(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create new user account with username and password")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse response = authService.registerUser(registerRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT access token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            boolean isValid = authService.validateToken(token);
            String email = isValid ? authService.getEmailFromToken(token) : null;
            
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "email", email != null ? email : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "email", ""
            ));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logout user (client-side token invalidation)")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request) {
        // Since we're using stateless JWT, logout is handled client-side by removing the token
        // This endpoint is mainly for consistency and future token blacklisting if needed
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("status", "success");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
