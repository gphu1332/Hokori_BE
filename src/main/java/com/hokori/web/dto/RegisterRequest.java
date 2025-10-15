package com.hokori.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Enhanced registration request with role selection
 */
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
    
    // Constructors
    public RegisterRequest() {}
    
    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getNativeLanguage() {
        return nativeLanguage;
    }
    
    public void setNativeLanguage(String nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }
    
    public String getCurrentJlptLevel() {
        return currentJlptLevel;
    }
    
    public void setCurrentJlptLevel(String currentJlptLevel) {
        this.currentJlptLevel = currentJlptLevel;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public String getTeachingExperience() {
        return teachingExperience;
    }
    
    public void setTeachingExperience(String teachingExperience) {
        this.teachingExperience = teachingExperience;
    }
    
    public String getQualifications() {
        return qualifications;
    }
    
    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
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