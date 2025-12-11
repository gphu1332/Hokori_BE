package com.hokori.web.controller;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.dto.*;
import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve current authenticated user's profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserProfile() {
        try {
            // Use native query to avoid LOB stream error
            String email = getCurrentUserEmail();
            var metadataOpt = userRepository.findUserProfileMetadataByEmail(email);
            if (metadataOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }
            
            Object[] metadata = metadataOpt.get();
            // Handle nested array case (PostgreSQL)
            Object[] actualMetadata = metadata;
            if (metadata.length == 1 && metadata[0] instanceof Object[]) {
                actualMetadata = (Object[]) metadata[0];
            }
            
            Map<String, Object> profile = createProfileResponseFromMetadata(actualMetadata);
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
            // Use native query to avoid LOB stream error
            var metadataOpt = userRepository.findUserProfileMetadataById(id);
            if (metadataOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }
            
            Object[] metadata = metadataOpt.get();
            // Handle nested array case (PostgreSQL)
            Object[] actualMetadata = metadata;
            if (metadata.length == 1 && metadata[0] instanceof Object[]) {
                actualMetadata = (Object[]) metadata[0];
            }
            
            Map<String, Object> profile = createProfileResponseFromMetadata(actualMetadata);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve user profile: " + e.getMessage());
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
            
            // Update email if provided (with validation)
            if (profileData.getEmail() != null && !profileData.getEmail().isBlank()) {
                String newEmail = profileData.getEmail().trim().toLowerCase();
                
                // Check if email is different from current email
                if (!newEmail.equals(currentUser.getEmail())) {
                    // Check if email already exists (excluding current user)
                    if (userRepository.findByEmail(newEmail).isPresent()) {
                        return ResponseEntity.ok(ApiResponse.error("Email already exists"));
                    }
                    
                    // Update email and set isVerified to false (user needs to verify new email)
                    currentUser.setEmail(newEmail);
                    currentUser.setIsVerified(false);
                    // Note: In production, you might want to send verification email here
                }
            }
            
            userService.updateUser(currentUser);
            
            // Use native query to avoid LOB stream error when returning response
            String email = getCurrentUserEmail();
            var metadataOpt = userRepository.findUserProfileMetadataByEmail(email);
            if (metadataOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }
            
            Object[] metadata = metadataOpt.get();
            Object[] actualMetadata = metadata;
            if (metadata.length == 1 && metadata[0] instanceof Object[]) {
                actualMetadata = (Object[]) metadata[0];
            }
            
            Map<String, Object> response = createProfileResponseFromMetadata(actualMetadata);
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
        // Use native query to avoid LOB stream error
        String email = getCurrentUserEmail();
        var metadataOpt = userRepository.findUserProfileMetadataByEmail(email);
        if (metadataOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("User not found"));
        }
        
        Object[] metadata = metadataOpt.get();
        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        Map<String, Object> teacherSection = createTeacherSectionFromMetadata(actualMetadata);
        Map<String, Object> data = new HashMap<>();
        data.put("teacher", teacherSection);
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
        if (req.getWebsiteUrl()!=null) u.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getLinkedin()!=null) u.setLinkedin(req.getLinkedin());
        if (req.getBankAccountNumber()!=null) u.setBankAccountNumber(req.getBankAccountNumber());
        if (req.getBankAccountName()!=null) u.setBankAccountName(req.getBankAccountName());
        if (req.getBankName()!=null) u.setBankName(req.getBankName());
        if (req.getBankBranchName()!=null) u.setBankBranchName(req.getBankBranchName());

        userService.updateUser(u);
        
        // Use native query to avoid LOB stream error when returning response
        String email = getCurrentUserEmail();
        var metadataOpt = userRepository.findUserProfileMetadataByEmail(email);
        if (metadataOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("User not found"));
        }
        
        Object[] metadata = metadataOpt.get();
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        Map<String, Object> response = createProfileResponseFromMetadata(actualMetadata);
        return ResponseEntity.ok(ApiResponse.success("Teacher section updated", response));
    }

    @Operation(summary = "Upload avatar cho user hiện tại")
    @PostMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public AvatarUploadRes uploadAvatar(@RequestPart("file") MultipartFile file) {
        Long userId = currentUserIdOrThrow();
        String url = userService.uploadAvatar(userId, file);
        return new AvatarUploadRes(url);
    }

    public record AvatarUploadRes(String avatarUrl) {}

    private Map<String, Object> createProfileResponse(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        // base fields
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("username", user.getUsername());
        m.put("displayName", user.getDisplayName());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("phoneNumber", user.getPhoneNumber());
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

            t.put("websiteUrl", user.getWebsiteUrl());
            t.put("linkedin", user.getLinkedin());

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

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return auth.getName();
    }
    
    private Long currentUserIdOrThrow() {
        String email = getCurrentUserEmail();
        // Use query that avoids LOB fields to prevent LOB stream errors
        var statusOpt = userRepository.findUserActiveStatusByEmail(email);
        if (statusOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Object[] status = statusOpt.get();
        // Handle nested array case (PostgreSQL)
        Object[] actualStatus = status;
        if (status.length == 1 && status[0] instanceof Object[]) {
            actualStatus = (Object[]) status[0];
        }
        if (actualStatus.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Long userId = ((Number) actualStatus[0]).longValue();
        Object isActiveObj = actualStatus[1];
        boolean isActive = false;
        if (isActiveObj instanceof Boolean) {
            isActive = (Boolean) isActiveObj;
        } else if (isActiveObj instanceof Number) {
            isActive = ((Number) isActiveObj).intValue() != 0;
        } else {
            String isActiveStr = isActiveObj.toString().toLowerCase().trim();
            isActive = "true".equals(isActiveStr) || "1".equals(isActiveStr);
        }
        if (!isActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }
        return userId;
    }
    
    /**
     * Create full profile response from metadata array (avoids LOB stream error).
     * Metadata array: [id, email, username, displayName, avatarUrl, phoneNumber, isActive, isVerified, 
     *                 lastLoginAt, createdAt, approvalStatus, approvedAt, profileApprovalRequestId,
     *                 yearsOfExperience, bio, websiteUrl, linkedin, bankAccountNumber, bankAccountName,
     *                 bankName, bankBranchName, lastPayoutDate]
     */
    private Map<String, Object> createProfileResponseFromMetadata(Object[] metadata) {
        Map<String, Object> m = new LinkedHashMap<>();
        
        // Base fields (indices 0-9)
        if (metadata.length > 0) m.put("id", ((Number) metadata[0]).longValue());
        if (metadata.length > 1) m.put("email", metadata[1]);
        if (metadata.length > 2) m.put("username", metadata[2]);
        if (metadata.length > 3) m.put("displayName", metadata[3]);
        if (metadata.length > 4) m.put("avatarUrl", metadata[4]);
        if (metadata.length > 5) m.put("phoneNumber", metadata[5]);
        if (metadata.length > 6) {
            Object isActiveObj = metadata[6];
            boolean isActive = false;
            if (isActiveObj instanceof Boolean) {
                isActive = (Boolean) isActiveObj;
            } else if (isActiveObj instanceof Number) {
                isActive = ((Number) isActiveObj).intValue() != 0;
            } else if (isActiveObj != null) {
                String isActiveStr = isActiveObj.toString().toLowerCase().trim();
                isActive = "true".equals(isActiveStr) || "1".equals(isActiveStr);
            }
            m.put("isActive", isActive);
        }
        if (metadata.length > 7) {
            Object isVerifiedObj = metadata[7];
            boolean isVerified = false;
            if (isVerifiedObj instanceof Boolean) {
                isVerified = (Boolean) isVerifiedObj;
            } else if (isVerifiedObj instanceof Number) {
                isVerified = ((Number) isVerifiedObj).intValue() != 0;
            } else if (isVerifiedObj != null) {
                String isVerifiedStr = isVerifiedObj.toString().toLowerCase().trim();
                isVerified = "true".equals(isVerifiedStr) || "1".equals(isVerifiedStr);
            }
            m.put("isVerified", isVerified);
        }
        if (metadata.length > 8) m.put("lastLoginAt", metadata[8]);
        if (metadata.length > 9) m.put("createdAt", metadata[9]);
        
        // Get role from SecurityContext
        String roleName = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            roleName = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(s -> s.startsWith("ROLE_"))
                    .map(s -> s.substring(5))
                    .findFirst()
                    .orElse(null);
        }
        m.put("role", roleName);
        
        // Teacher section (if teacher role or has approval flow)
        boolean isTeacherRole = "TEACHER".equals(roleName);
        boolean hasTeacherFlow = metadata.length > 10 && metadata[10] != null 
                && !ApprovalStatus.NONE.name().equals(((String) metadata[10]).toUpperCase());
        
        if (isTeacherRole || hasTeacherFlow) {
            Map<String, Object> teacherSection = createTeacherSectionFromMetadata(metadata);
            m.put("teacher", teacherSection);
        }
        
        m.put("status", "success");
        m.put("timestamp", java.time.LocalDateTime.now());
        return m;
    }
    
    /**
     * Create teacher section from metadata array (avoids LOB stream error).
     * Metadata array: [id, email, username, displayName, avatarUrl, phoneNumber, isActive, isVerified, 
     *                 lastLoginAt, createdAt, approvalStatus, approvedAt, profileApprovalRequestId,
     *                 yearsOfExperience, bio, websiteUrl, linkedin, bankAccountNumber, bankAccountName,
     *                 bankName, bankBranchName, lastPayoutDate]
     */
    private Map<String, Object> createTeacherSectionFromMetadata(Object[] metadata) {
        Map<String, Object> t = new LinkedHashMap<>();
        
        // Index mapping:
        // 10: approvalStatus, 11: approvedAt, 12: profileApprovalRequestId,
        // 13: yearsOfExperience, 14: bio, 15: websiteUrl, 16: linkedin,
        // 17: bankAccountNumber, 18: bankAccountName, 19: bankName, 20: bankBranchName, 21: lastPayoutDate
        
        if (metadata.length > 10 && metadata[10] != null) {
            try {
                t.put("approvalStatus", ApprovalStatus.valueOf(((String) metadata[10]).toUpperCase()));
            } catch (Exception e) {
                t.put("approvalStatus", null);
            }
        }
        if (metadata.length > 11) {
            t.put("approvedAt", metadata[11]);
        }
        if (metadata.length > 12) {
            t.put("currentApproveRequestId", metadata[12] != null ? ((Number) metadata[12]).longValue() : null);
        }
        if (metadata.length > 13) {
            t.put("yearsOfExperience", metadata[13] != null ? ((Number) metadata[13]).intValue() : null);
        }
        if (metadata.length > 14) {
            t.put("bio", metadata[14]); // bio field - loaded via native query to avoid LOB stream error
        }
        if (metadata.length > 15) {
            t.put("websiteUrl", metadata[15]);
        }
        if (metadata.length > 16) {
            t.put("linkedin", metadata[16]);
        }
        if (metadata.length > 17) {
            t.put("bankAccountNumber", metadata[17]);
        }
        if (metadata.length > 18) {
            t.put("bankAccountName", metadata[18]);
        }
        if (metadata.length > 19) {
            t.put("bankName", metadata[19]);
        }
        if (metadata.length > 20) {
            t.put("bankBranchName", metadata[20]);
        }
        if (metadata.length > 21) {
            t.put("lastPayoutDate", metadata[21]);
        }
        
        return t;
    }
}