package com.hokori.web.dto;

import jakarta.validation.constraints.NotNull;

public class UserStatusRequest {
    
    @NotNull(message = "isActive status is required")
    private Boolean isActive;
    
    // Constructors
    public UserStatusRequest() {}
    
    public UserStatusRequest(Boolean isActive) {
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "UserStatusRequest{" +
                "isActive=" + isActive +
                '}';
    }
}
