package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.AuthResponse;
import com.hokori.web.dto.LoginRequest;
import com.hokori.web.dto.FirebaseAuthRequest;
import com.hokori.web.dto.RegisterRequest;
import com.hokori.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user with role selection and password confirmation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Validate registration request
            if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Username is required"));
            }
            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Email is required"));
            }
            if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Password is required"));
            }
            if (registerRequest.getConfirmPassword() == null || registerRequest.getConfirmPassword().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Password confirmation is required"));
            }
            
            // Validate password confirmation
            if (!registerRequest.isPasswordConfirmed()) {
                return ResponseEntity.ok(ApiResponse.error("Password and confirmation do not match"));
            }
            
            // Validate role selection
            if (!registerRequest.isValidRole()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid role selected. Available roles: LEARNER, TEACHER, STAFF, ADMIN"));
            }
            
            // Register user
            AuthResponse authResponse = authService.registerUser(registerRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Registration successful");
            response.put("role", registerRequest.getRoleName());
            
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with username/email and password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.authenticateWithUsernamePassword(loginRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Login successful");
            response.put("roles", authResponse.getRoles());
            
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/firebase")
    @Operation(summary = "Firebase authentication", description = "Authenticate user with Firebase token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest firebaseRequest) {
        try {
            AuthResponse authResponse = authService.authenticateUser(firebaseRequest.getFirebaseToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Firebase authentication successful");
            response.put("roles", authResponse.getRoles());
            
            return ResponseEntity.ok(ApiResponse.success("Firebase authentication successful", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Firebase authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/roles")
    @Operation(summary = "Get available roles", description = "Get list of available roles for registration")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableRoles() {
        try {
            List<Map<String, Object>> roles = List.of(
                Map.of("roleName", "LEARNER", "description", "Regular student/learner"),
                Map.of("roleName", "TEACHER", "description", "Teacher who can create content"),
                Map.of("roleName", "STAFF", "description", "Staff member with limited admin access"),
                Map.of("roleName", "ADMIN", "description", "Full system administrator")
            );
            
            return ResponseEntity.ok(ApiResponse.success("Available roles retrieved successfully", roles));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve roles: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        try {
            // This endpoint requires authentication, so we'll get user from SecurityContext
            // For now, return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Current user endpoint - requires authentication");
            response.put("note", "Use JWT token in Authorization header");
            
            return ResponseEntity.ok(ApiResponse.success("Current user endpoint", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to get current user: " + e.getMessage()));
        }
    }
}