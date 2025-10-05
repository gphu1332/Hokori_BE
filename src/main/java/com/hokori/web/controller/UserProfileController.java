package com.hokori.web.controller;

import com.hokori.web.entity.User;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile", description = "Get user profile information")
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

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile", description = "Update user profile information")
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
            if (profileData.containsKey("nativeLanguage")) {
                user.setNativeLanguage((String) profileData.get("nativeLanguage"));
            }
            if (profileData.containsKey("learningLanguage")) {
                user.setLearningLanguage((String) profileData.get("learningLanguage"));
            }
            if (profileData.containsKey("dateOfBirth")) {
                String dobStr = (String) profileData.get("dateOfBirth");
                if (dobStr != null && !dobStr.isEmpty()) {
                    user.setDateOfBirth(LocalDate.parse(dobStr));
                }
            }
            if (profileData.containsKey("gender")) {
                String genderStr = (String) profileData.get("gender");
                if (genderStr != null) {
                    user.setGender(User.Gender.valueOf(genderStr.toUpperCase()));
                }
            }
            if (profileData.containsKey("currentJlptLevel")) {
                String levelStr = (String) profileData.get("currentJlptLevel");
                if (levelStr != null) {
                    user.setCurrentJlptLevel(User.JLPTLevel.valueOf(levelStr.toUpperCase()));
                }
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

    @PutMapping("/{id}/avatar")
    @Operation(summary = "Update user avatar", description = "Update user avatar URL")
    public ResponseEntity<Map<String, Object>> updateUserAvatar(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody Map<String, String> avatarData) {
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
            String avatarUrl = avatarData.get("avatarUrl");
            
            if (avatarUrl != null) {
                user.setAvatarUrl(avatarUrl);
                User updatedUser = userService.updateUser(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Avatar updated successfully");
                response.put("avatarUrl", updatedUser.getAvatarUrl());
                response.put("status", "success");
                response.put("timestamp", LocalDateTime.now());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Avatar URL is required");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update avatar");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Change user password", description = "Change user password")
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

    @PutMapping("/{id}/preferences")
    @Operation(summary = "Update user preferences", description = "Update user learning preferences")
    public ResponseEntity<Map<String, Object>> updateUserPreferences(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody Map<String, Object> preferences) {
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
            
            // Update learning preferences
            if (preferences.containsKey("currentJlptLevel")) {
                String levelStr = (String) preferences.get("currentJlptLevel");
                if (levelStr != null) {
                    user.setCurrentJlptLevel(User.JLPTLevel.valueOf(levelStr.toUpperCase()));
                }
            }
            if (preferences.containsKey("learningLanguage")) {
                user.setLearningLanguage((String) preferences.get("learningLanguage"));
            }
            if (preferences.containsKey("nativeLanguage")) {
                user.setNativeLanguage((String) preferences.get("nativeLanguage"));
            }

            User updatedUser = userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Preferences updated successfully");
            response.put("preferences", createPreferencesResponse(updatedUser));
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update preferences");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/preferences")
    @Operation(summary = "Get user preferences", description = "Get user learning preferences")
    public ResponseEntity<Map<String, Object>> getUserPreferences(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("preferences", createPreferencesResponse(user));
                response.put("status", "success");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve preferences");
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
        profile.put("dateOfBirth", user.getDateOfBirth());
        profile.put("gender", user.getGender());
        profile.put("country", user.getCountry());
        profile.put("nativeLanguage", user.getNativeLanguage());
        profile.put("learningLanguage", user.getLearningLanguage());
        profile.put("currentJlptLevel", user.getCurrentJlptLevel());
        profile.put("isActive", user.getIsActive());
        profile.put("isVerified", user.getIsVerified());
        profile.put("lastLoginAt", user.getLastLoginAt());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("status", "success");
        profile.put("timestamp", LocalDateTime.now());
        return profile;
    }

    private Map<String, Object> createPreferencesResponse(User user) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("currentJlptLevel", user.getCurrentJlptLevel());
        preferences.put("learningLanguage", user.getLearningLanguage());
        preferences.put("nativeLanguage", user.getNativeLanguage());
        return preferences;
    }
}
