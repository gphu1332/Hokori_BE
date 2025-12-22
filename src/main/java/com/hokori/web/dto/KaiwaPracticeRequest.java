package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Kaiwa (Conversation) Practice
 * User provides target text and their recorded audio
 * 
 * <h3>Business Rules & Limits:</h3>
 * <ul>
 *   <li><b>Target Text:</b> Max 5000 characters (Japanese text to practice)</li>
 *   <li><b>Audio Data:</b> Max 1.3MB base64 string (~1MB decoded file, ~60 seconds duration)</li>
 *   <li><b>Audio Format:</b> wav, mp3, flac, ogg, webm (auto-detected from magic bytes)</li>
 *   <li><b>Transcript:</b> Auto-generated from audio via Google Cloud Speech-to-Text API
 *       <ul>
 *         <li>Typical: 150-200 characters for 60 seconds of speech</li>
 *         <li>Maximum: ~1000 characters if speaking very fast</li>
 *         <li>Validation: Transcript length is validated (max 5000 chars) after STT</li>
 *       </ul>
 *   </li>
 *   <li><b>Google Cloud Limits:</b>
 *       <ul>
 *         <li>File size: 10MB decoded (13MB base64) for long-running recognition</li>
 *         <li>Duration: 60 seconds maximum (Google Cloud synchronous API limit)</li>
 *         <li>Note: Audio > 60s will be rejected. User should record shorter audio or split into multiple recordings.</li>
 *         <li>Backend validates both before processing</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Validation Flow:</h3>
 * <ol>
 *   <li>DTO validation: Base64 string length ≤ 13MB</li>
 *   <li>Service validation: Decoded file size ≤ 10MB</li>
 *   <li>Service validation: Estimated duration ≤ 60 seconds</li>
 *   <li>After STT: Transcript length ≤ 5000 characters</li>
 * </ol>
 */
@Schema(description = "Request for practicing Japanese conversation (Kaiwa)")
public class KaiwaPracticeRequest {
    
    @NotBlank(message = "Target text is required")
    @Size(max = 5000, message = "Target text must not exceed 5000 characters")
    @Schema(description = "The Japanese text user should practice",
            example = "こんにちは、私は日本語を勉強しています",
            required = true)
    private String targetText; // The Japanese text user should practice
    
    @NotBlank(message = "Audio data is required")
    @Size(max = 1400000, message = "Audio data must not exceed 1.3MB base64 (supports up to 60 seconds of audio)") // ~1.3MB base64 ≈ 1MB decoded (supports up to 60 seconds)
    @Schema(description = "Base64 encoded audio of user's pronunciation (max 1.3MB base64 = ~1MB decoded = ~60 seconds)",
            example = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=",
            required = true)
    private String audioData; // Base64 encoded audio of user's pronunciation
    
    @Schema(description = "JLPT level",
            example = "N5",
            allowableValues = {"N5", "N4", "N3", "N2", "N1"},
            defaultValue = "N5")
    private String level; // Optional, JLPT level: N5, N4, N3, N2, N1 (default: N5)
    
    @Schema(description = "Language code",
            example = "ja-JP",
            allowableValues = {"ja-JP", "vi-VN", "en-US"},
            defaultValue = "ja-JP")
    private String language; // Optional, default to "ja-JP"
    
    @Schema(description = "Audio format",
            example = "wav",
            allowableValues = {"wav", "mp3", "flac", "ogg"},
            defaultValue = "wav")
    private String audioFormat; // Optional, default to "wav"
    
    @Schema(description = "Voice name for reference audio generation",
            example = "ja-JP-Standard-A",
            allowableValues = {"ja-JP-Standard-A", "ja-JP-Standard-B", "ja-JP-Standard-C", "ja-JP-Standard-D"},
            defaultValue = "ja-JP-Standard-A")
    private String voice; // Optional, default to "ja-JP-Standard-A" (for reference audio generation)
    
    @Schema(description = "Speech speed for reference audio",
            example = "normal",
            allowableValues = {"slow", "normal", "fast"},
            defaultValue = "normal")
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
               audioFormat.equals("ogg") ||
               audioFormat.equals("webm");
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

