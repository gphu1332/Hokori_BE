package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for converting text to speech")
public class TextToSpeechRequest {
    
    @NotBlank(message = "Text is required")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    @Schema(description = "Text to convert to speech",
            example = "こんにちは、私は日本語を勉強しています",
            required = true)
    private String text;
    
    @Schema(description = "Voice name for text-to-speech",
            example = "ja-JP-Standard-A",
            allowableValues = {"ja-JP-Standard-A", "ja-JP-Standard-B", "ja-JP-Standard-C", "ja-JP-Standard-D"},
            defaultValue = "ja-JP-Standard-A")
    private String voice; // Optional, default to "ja-JP-Standard-A"
    
    @Schema(description = "Speech speed",
            example = "normal",
            allowableValues = {"slow", "normal", "fast"},
            defaultValue = "normal")
    private String speed; // Optional, default to "normal"
    
    @Schema(description = "Audio output format",
            example = "mp3",
            allowableValues = {"mp3", "wav", "ogg"},
            defaultValue = "mp3")
    private String audioFormat; // Optional, default to "mp3"
    
    // Constructors
    public TextToSpeechRequest() {}
    
    public TextToSpeechRequest(String text, String voice, String speed, String audioFormat) {
        this.text = text;
        this.voice = voice;
        this.speed = speed;
        this.audioFormat = audioFormat;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
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
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    // Validation methods
    public boolean isValidSpeed() {
        return speed == null || 
               speed.equals("slow") || 
               speed.equals("normal") || 
               speed.equals("fast");
    }
    
    public boolean isValidAudioFormat() {
        return audioFormat == null || 
               audioFormat.equals("mp3") || 
               audioFormat.equals("wav") || 
               audioFormat.equals("ogg");
    }
    
    @Override
    public String toString() {
        return "TextToSpeechRequest{" +
                "text='" + text + '\'' +
                ", voice='" + voice + '\'' +
                ", speed='" + speed + '\'' +
                ", audioFormat='" + audioFormat + '\'' +
                '}';
    }
}
