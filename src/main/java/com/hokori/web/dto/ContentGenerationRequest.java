package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContentGenerationRequest {
    
    @NotBlank(message = "Prompt is required")
    @Size(max = 2000, message = "Prompt must not exceed 2000 characters")
    private String prompt;
    
    private String contentType; // "lesson", "exercise", "explanation", "general"
    
    // Constructors
    public ContentGenerationRequest() {}
    
    public ContentGenerationRequest(String prompt, String contentType) {
        this.prompt = prompt;
        this.contentType = contentType;
    }
    
    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    // Validation method
    public boolean isValidContentType() {
        return contentType == null || 
               contentType.equals("lesson") || 
               contentType.equals("exercise") || 
               contentType.equals("explanation") || 
               contentType.equals("general");
    }
    
    @Override
    public String toString() {
        return "ContentGenerationRequest{" +
                "prompt='" + prompt + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
