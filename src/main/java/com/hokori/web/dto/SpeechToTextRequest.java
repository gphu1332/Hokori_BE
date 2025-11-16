package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for converting speech to text")
public class SpeechToTextRequest {
    
    @NotBlank(message = "Audio data is required")
    @Size(max = 10485760, message = "Audio data must not exceed 10MB") // 10MB limit
    @Schema(description = "Base64 encoded audio data",
            example = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=",
            required = true)
    private String audioData; // Base64 encoded audio
    
    @Schema(description = "Language code for speech recognition",
            example = "ja-JP",
            allowableValues = {"ja-JP", "vi-VN", "en-US"},
            defaultValue = "ja-JP")
    private String language; // Optional, default to "ja-JP"

    @Schema(description = "Audio format",
            example = "wav",
            allowableValues = {"wav", "mp3", "flac", "ogg"},
            defaultValue = "wav")
    private String audioFormat; // Optional, default to "wav"
    
    // Constructors
    public SpeechToTextRequest() {}
    
    public SpeechToTextRequest(String audioData, String language, String audioFormat) {
        this.audioData = audioData;
        this.language = language;
        this.audioFormat = audioFormat;
    }
    
    // Getters and Setters
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
    
    // Validation method
    public boolean isValidAudioFormat() {
        return audioFormat == null || 
               audioFormat.equals("wav") || 
               audioFormat.equals("mp3") || 
               audioFormat.equals("flac") || 
               audioFormat.equals("ogg");
    }
    
    @Override
    public String toString() {
        return "SpeechToTextRequest{" +
                "audioData='" + (audioData != null ? "[BASE64_DATA]" : "null") + '\'' +
                ", language='" + language + '\'' +
                ", audioFormat='" + audioFormat + '\'' +
                '}';
    }
}
