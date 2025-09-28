package com.hokori.web.dto;

import com.hokori.web.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo user;
    private List<String> roles;
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.roles = roles;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    
    // Inner class for user information
    public static class UserInfo {
        private Long id;
        private String firebaseUid;
        private String email;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String phoneNumber;
        private String country;
        private String nativeLanguage;
        private String learningLanguage;
        private String currentJlptLevel;
        private Boolean isActive;
        private Boolean isVerified;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        
        // Constructors
        public UserInfo() {}
        
        public UserInfo(User user) {
            this.id = user.getId();
            this.firebaseUid = user.getFirebaseUid();
            this.email = user.getEmail();
            this.username = user.getUsername();
            this.displayName = user.getDisplayName();
            this.avatarUrl = user.getAvatarUrl();
            this.phoneNumber = user.getPhoneNumber();
            this.country = user.getCountry();
            this.nativeLanguage = user.getNativeLanguage();
            this.learningLanguage = user.getLearningLanguage();
            this.currentJlptLevel = user.getCurrentJlptLevel() != null ? user.getCurrentJlptLevel().name() : null;
            this.isActive = user.getIsActive();
            this.isVerified = user.getIsVerified();
            this.lastLoginAt = user.getLastLoginAt();
            this.createdAt = user.getCreatedAt();
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getFirebaseUid() {
            return firebaseUid;
        }
        
        public void setFirebaseUid(String firebaseUid) {
            this.firebaseUid = firebaseUid;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getAvatarUrl() {
            return avatarUrl;
        }
        
        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
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
        
        public String getLearningLanguage() {
            return learningLanguage;
        }
        
        public void setLearningLanguage(String learningLanguage) {
            this.learningLanguage = learningLanguage;
        }
        
        public String getCurrentJlptLevel() {
            return currentJlptLevel;
        }
        
        public void setCurrentJlptLevel(String currentJlptLevel) {
            this.currentJlptLevel = currentJlptLevel;
        }
        
        public Boolean getIsActive() {
            return isActive;
        }
        
        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
        
        public Boolean getIsVerified() {
            return isVerified;
        }
        
        public void setIsVerified(Boolean isVerified) {
            this.isVerified = isVerified;
        }
        
        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }
        
        public void setLastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", user=" + user +
                ", roles=" + roles +
                '}';
    }
}
