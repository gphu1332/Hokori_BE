package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SentimentAnalysisRequest {
    
    @NotBlank(message = "Text is required")
    @Size(max = 10000, message = "Text must not exceed 10000 characters")
    private String text;
    
    // Constructors
    public SentimentAnalysisRequest() {}
    
    public SentimentAnalysisRequest(String text) {
        this.text = text;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "SentimentAnalysisRequest{" +
                "text='" + text + '\'' +
                '}';
    }
}
