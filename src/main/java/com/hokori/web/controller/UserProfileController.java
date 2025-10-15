package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.UserProfileUpdateRequest;
import com.hokori.web.dto.PasswordChangeRequest;
import com.hokori.web.entity.User;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "User Profile", description = "User profile management endpoints")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve current authenticated user's profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserProfile() {
        try {
            User currentUser = currentUserService.getCurrentUserOrThrow();
            Map<String, Object> profile = createProfileResponse(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", profile));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to get current user profile: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID", description = "Retrieve user profile by ID")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> profile = createProfileResponse(user);
                return ResponseEntity.ok(profile);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve user profile");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update current authenticated user's profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCurrentUserProfile(@Valid @RequestBody UserProfileUpdateRequest profileData) {
        try {
            User currentUser = currentUserService.getCurrentUserOrThrow();
            
            // Update basic profile information
            if (profileData.getDisplayName() != null) {
                currentUser.setDisplayName(profileData.getDisplayName());
            }
            if (profileData.getPhoneNumber() != null) {
                currentUser.setPhoneNumber(profileData.getPhoneNumber());
            }
            if (profileData.getCountry() != null) {
                currentUser.setCountry(profileData.getCountry());
            }
            
            User updatedUser = userService.updateUser(currentUser);
            Map<String, Object> response = createProfileResponse(updatedUser);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to update profile: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile by ID", description = "Update user profile information by ID")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @Parameter(description = "User ID") @PathVariable Long id, 
            @RequestBody Map<String, Object> profileData) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            
            // Update basic profile information
            if (profileData.containsKey("displayName")) {
                user.setDisplayName((String) profileData.get("displayName"));
            }
            if (profileData.containsKey("phoneNumber")) {
                user.setPhoneNumber((String) profileData.get("phoneNumber"));
            }
            if (profileData.containsKey("country")) {
                user.setCountry((String) profileData.get("country"));
            }

            User updatedUser = userService.updateUser(user);
            Map<String, Object> response = createProfileResponse(updatedUser);
            response.put("message", "Profile updated successfully");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update profile: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change current user password", description = "Change current authenticated user's password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeCurrentUserPassword(@Valid @RequestBody PasswordChangeRequest passwordData) {
        try {
            User currentUser = currentUserService.getCurrentUserOrThrow();
            
            if (!passwordData.isPasswordConfirmed()) {
                return ResponseEntity.ok(ApiResponse.error("New password and confirmation do not match"));
            }

            // Verify current password
            if (currentUser.getPasswordHash() != null && 
                !passwordEncoder.matches(passwordData.getCurrentPassword(), currentUser.getPasswordHash())) {
                return ResponseEntity.ok(ApiResponse.error("Current password is incorrect"));
            }

            // Update password
            currentUser.setPasswordHash(passwordEncoder.encode(passwordData.getNewPassword()));
            userService.updateUser(currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to change password: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Change user password by ID", description = "Change user password by ID")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Parameter(description = "User ID") @PathVariable Long id, 
            @RequestBody Map<String, String> passwordData) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (currentPassword == null || newPassword == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Current password and new password are required");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Verify current password
            if (user.getPasswordHash() != null && 
                !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Current password is incorrect");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to change password");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Map<String, Object> createProfileResponse(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("username", user.getUsername());
        profile.put("displayName", user.getDisplayName());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("country", user.getCountry());
        profile.put("isActive", user.getIsActive());
        profile.put("isVerified", user.getIsVerified());
        profile.put("lastLoginAt", user.getLastLoginAt());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("status", "success");
        profile.put("timestamp", LocalDateTime.now());
        return profile;
    }
}