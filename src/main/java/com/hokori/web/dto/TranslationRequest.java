package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for translating text between languages")
public class TranslationRequest {
    
    @NotBlank(message = "Text is required")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    @Schema(description = "Text to translate",
            example = "私は日本語を勉強しています",
            required = true)
    private String text;
    
    @Schema(description = "Source language code (auto-detect if null)",
            example = "ja",
            allowableValues = {"ja", "vi", "en", "ko", "zh"})
    private String sourceLanguage; // Optional, auto-detect if null
    
    @Schema(description = "Target language code (default to English if null)",
            example = "vi",
            allowableValues = {"ja", "vi", "en", "ko", "zh"},
            defaultValue = "en")
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
