package com.hokori.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UserProfileUpdateRequest {
    
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
    
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
    
    @Size(max = 50, message = "Native language must not exceed 50 characters")
    private String nativeLanguage;
    
    @Size(max = 50, message = "Learning language must not exceed 50 characters")
    private String learningLanguage;
    
    private LocalDate dateOfBirth;
    
    private String gender; // MALE, FEMALE, OTHER
    
    private String currentJlptLevel; // N5, N4, N3, N2, N1
    
    // Constructors
    public UserProfileUpdateRequest() {}
    
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
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getCurrentJlptLevel() {
        return currentJlptLevel;
    }
    
    public void setCurrentJlptLevel(String currentJlptLevel) {
        this.currentJlptLevel = currentJlptLevel;
    }
}
