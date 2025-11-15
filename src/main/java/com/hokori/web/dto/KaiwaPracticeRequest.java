package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Kaiwa (Conversation) Practice
 * User provides target text and their recorded audio
 */
public class KaiwaPracticeRequest {
    
    @NotBlank(message = "Target text is required")
    @Size(max = 5000, message = "Target text must not exceed 5000 characters")
    private String targetText; // The Japanese text user should practice
    
    @NotBlank(message = "Audio data is required")
    @Size(max = 10485760, message = "Audio data must not exceed 10MB") // 10MB limit
    private String audioData; // Base64 encoded audio of user's pronunciation
    
    private String level; // Optional, JLPT level: N5, N4, N3, N2, N1 (default: N5)
    private String language; // Optional, default to "ja-JP"
    private String audioFormat; // Optional, default to "wav"
    private String voice; // Optional, default to "ja-JP-Standard-A" (for reference audio generation)
    private String speed; // Optional, default to "normal" (for reference audio generation)
    
    // Constructors
    public KaiwaPracticeRequest() {}
    
    public KaiwaPracticeRequest(String targetText, String audioData, String language, String audioFormat) {
        this.targetText = targetText;
        this.audioData = audioData;
        this.language = language;
        this.audioFormat = audioFormat;
    }
    
    // Getters and Setters
    public String getTargetText() {
        return targetText;
    }
    
    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }
    
    public String getAudioData() {
        return audioData;
    }
    
    public void setAudioData(String audioData) {
        this.audioData = audioData;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public String getVoice() {
        return voice;
    }
    
    public void setVoice(String voice) {
        this.voice = voice;
    }
    
    public String getSpeed() {
        return speed;
    }
    
    public void setSpeed(String speed) {
        this.speed = speed;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    // Validation methods
    public boolean isValidAudioFormat() {
        return audioFormat == null || 
               audioFormat.equals("wav") || 
               audioFormat.equals("mp3") || 
               audioFormat.equals("flac") || 
               audioFormat.equals("ogg");
    }
    
    public boolean isValidSpeed() {
        return speed == null || 
               speed.equals("slow") || 
               speed.equals("normal") || 
               speed.equals("fast");
    }
    
    public boolean isValidLevel() {
        if (level == null || level.isEmpty()) {
            return true; // Optional field
        }
        String upperLevel = level.toUpperCase();
        return upperLevel.equals("N5") || 
               upperLevel.equals("N4") || 
               upperLevel.equals("N3") || 
               upperLevel.equals("N2") || 
               upperLevel.equals("N1");
    }
    
    @Override
    public String toString() {
        return "KaiwaPracticeRequest{" +
                "targetText='" + targetText + '\'' +
                ", audioData='" + (audioData != null ? "[BASE64_DATA]" : "null") + '\'' +
                ", level='" + level + '\'' +
                ", language='" + language + '\'' +
                ", audioFormat='" + audioFormat + '\'' +
                ", voice='" + voice + '\'' +
                ", speed='" + speed + '\'' +
                '}';
    }
}

