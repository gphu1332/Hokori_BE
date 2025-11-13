package com.hokori.web.controller;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.dto.*;
import com.hokori.web.entity.User;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeCurrentUserPassword(@Valid @RequestBody ChangePasswordRequest passwordData) {
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

    @GetMapping("/me/teacher")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @Operation(summary = "Get current teacher section", description = "Return teacher-only profile fields")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentTeacherSection() {
        User u = currentUserService.getCurrentUserOrThrow();
        Map<String, Object> full = createProfileResponse(u);
        Object teacher = full.get("teacher"); // null nếu không phải teacher
        Map<String, Object> data = new HashMap<>();
        data.put("teacher", teacher);
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @PutMapping("/me/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Update current teacher section", description = "Update teacher-only profile fields")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCurrentTeacherSection(
            @Valid @RequestBody com.hokori.web.dto.TeacherProfileUpdateRequest req) {
        User u = currentUserService.getCurrentUserOrThrow();

        if (req.getYearsOfExperience()!=null) u.setYearsOfExperience(req.getYearsOfExperience());
        if (req.getBio()!=null) u.setBio(req.getBio());
        if (req.getTeachingStyles()!=null) u.setTeachingStyles(req.getTeachingStyles());

        if (req.getWebsiteUrl()!=null) u.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getFacebook()!=null) u.setFacebook(req.getFacebook());
        if (req.getInstagram()!=null) u.setInstagram(req.getInstagram());
        if (req.getLinkedin()!=null) u.setLinkedin(req.getLinkedin());
        if (req.getTiktok()!=null) u.setTiktok(req.getTiktok());
        if (req.getX()!=null) u.setX(req.getX());
        if (req.getYoutube()!=null) u.setYoutube(req.getYoutube());

        if (req.getBankAccountNumber()!=null) u.setBankAccountNumber(req.getBankAccountNumber());
        if (req.getBankAccountName()!=null) u.setBankAccountName(req.getBankAccountName());
        if (req.getBankName()!=null) u.setBankName(req.getBankName());
        if (req.getBankBranchName()!=null) u.setBankBranchName(req.getBankBranchName());

        User saved = userService.updateUser(u);
        return ResponseEntity.ok(ApiResponse.success("Teacher section updated", createProfileResponse(saved)));
    }

//    @PostMapping("/me/teacher/submit-approval")
//    @PreAuthorize("hasRole('TEACHER')")
//    @Operation(summary = "Submit teacher approval request")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> submitTeacherApproval() {
//        User u = currentUserService.getCurrentUserOrThrow();
//        Map<String, Object> result = userService.submitTeacherApproval(u.getId());
//        return ResponseEntity.ok(ApiResponse.success("Submitted", result));
//    }



    private Map<String, Object> createProfileResponse(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        // base fields
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("username", user.getUsername());
        m.put("displayName", user.getDisplayName());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("phoneNumber", user.getPhoneNumber());
        m.put("country", user.getCountry());
        m.put("isActive", user.getIsActive());
        m.put("isVerified", user.getIsVerified());
        m.put("lastLoginAt", user.getLastLoginAt());
        m.put("createdAt", user.getCreatedAt());

        // lấy role từ SecurityContext (tránh lazy load Role)
        String roleName = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            roleName = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority) // "ROLE_TEACHER"
                    .filter(s -> s.startsWith("ROLE_"))
                    .map(s -> s.substring(5))            // "TEACHER"
                    .findFirst()
                    .orElse(null);
        }
        m.put("role", roleName);

        // trả teacher section nếu là teacher hoặc có flow duyệt
        boolean isTeacherRole = "TEACHER".equals(roleName);
        boolean hasTeacherFlow = user.getApprovalStatus() != null && user.getApprovalStatus() != ApprovalStatus.NONE;

        if (isTeacherRole || hasTeacherFlow) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("approvalStatus", user.getApprovalStatus());
            t.put("approvedAt", user.getApprovedAt());
            t.put("currentApproveRequestId",
                    user.getCurrentApproveRequest() != null ? user.getCurrentApproveRequest().getId() : null);

            t.put("yearsOfExperience", user.getYearsOfExperience());
            t.put("bio", user.getBio());
            t.put("teachingStyles", user.getTeachingStyles());

            t.put("websiteUrl", user.getWebsiteUrl());
            t.put("facebook", user.getFacebook());
            t.put("instagram", user.getInstagram());
            t.put("linkedin", user.getLinkedin());
            t.put("tiktok", user.getTiktok());
            t.put("x", user.getX());
            t.put("youtube", user.getYoutube());

            t.put("bankAccountNumber", user.getBankAccountNumber());
            t.put("bankAccountName", user.getBankAccountName());
            t.put("bankName", user.getBankName());
            t.put("bankBranchName", user.getBankBranchName());
            t.put("lastPayoutDate", user.getLastPayoutDate());

            m.put("teacher", t);
        }

        // giữ key cho FE cũ
        m.put("status", "success");
        m.put("timestamp", java.time.LocalDateTime.now());
        return m;
    }
}