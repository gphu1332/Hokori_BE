package com.hokori.web.dto;

import jakarta.validation.constraints.Size;

public class UserPreferencesRequest {
    
    private String currentJlptLevel; // N5, N4, N3, N2, N1
    
    @Size(max = 50, message = "Learning language must not exceed 50 characters")
    private String learningLanguage;
    
    @Size(max = 50, message = "Native language must not exceed 50 characters")
    private String nativeLanguage;
    
    // Constructors
    public UserPreferencesRequest() {}
    
    public UserPreferencesRequest(String currentJlptLevel, String learningLanguage, String nativeLanguage) {
        this.currentJlptLevel = currentJlptLevel;
        this.learningLanguage = learningLanguage;
        this.nativeLanguage = nativeLanguage;
    }
    
    // Getters and Setters
    public String getCurrentJlptLevel() {
        return currentJlptLevel;
    }
    
    public void setCurrentJlptLevel(String currentJlptLevel) {
        this.currentJlptLevel = currentJlptLevel;
    }
    
    public String getLearningLanguage() {
        return learningLanguage;
    }
    
    public void setLearningLanguage(String learningLanguage) {
        this.learningLanguage = learningLanguage;
    }
    
    public String getNativeLanguage() {
        return nativeLanguage;
    }
    
    public void setNativeLanguage(String nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }
}
