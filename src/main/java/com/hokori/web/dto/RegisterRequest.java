package com.hokori.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Enhanced registration request with role selection
 */

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;
    
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
    
    @Size(max = 50, message = "Native language must not exceed 50 characters")
    private String nativeLanguage;
    
    private String currentJlptLevel;
    
    // Role selection - default to LEARNER
    private String roleName = "LEARNER";
    
    // Additional fields for teacher registration
    private String teachingExperience;
    private String qualifications;
    private String bio;
    
    /**
     * Validate role selection
     */
    public boolean isValidRole() {
        return roleName != null && 
               (roleName.equals("LEARNER") || roleName.equals("TEACHER") || 
                roleName.equals("STAFF") || roleName.equals("ADMIN"));
    }
    
    /**
     * Check if this is a teacher registration
     */
    public boolean isTeacherRegistration() {
        return "TEACHER".equals(roleName);
    }
    
    /**
     * Validate password confirmation
     */
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
    
    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}