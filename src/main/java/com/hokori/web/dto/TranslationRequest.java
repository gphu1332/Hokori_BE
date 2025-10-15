package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TranslationRequest {
    
    @NotBlank(message = "Text is required")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    private String text;
    
    private String sourceLanguage; // Optional, auto-detect if null
    private String targetLanguage; // Optional, default to English if null
    
    // Constructors
    public TranslationRequest() {}
    
    public TranslationRequest(String text, String sourceLanguage, String targetLanguage) {
        this.text = text;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getSourceLanguage() {
        return sourceLanguage;
    }
    
    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }
    
    public String getTargetLanguage() {
        return targetLanguage;
    }
    
    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
    
    @Override
    public String toString() {
        return "TranslationRequest{" +
                "text='" + text + '\'' +
                ", sourceLanguage='" + sourceLanguage + '\'' +
                ", targetLanguage='" + targetLanguage + '\'' +
                '}';
    }
}
