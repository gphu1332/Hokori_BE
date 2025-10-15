package com.hokori.web.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {
    
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
    
    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;
    
    // Constructors
    public UserProfileUpdateRequest() {}
    
    public UserProfileUpdateRequest(String displayName, String phoneNumber, String country) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.country = country;
    }
    
    // Getters and Setters
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
    
    @Override
    public String toString() {
        return "UserProfileUpdateRequest{" +
                "displayName='" + displayName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}