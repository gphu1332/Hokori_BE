package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TextToSpeechRequest {
    
    @NotBlank(message = "Text is required")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    private String text;
    
    private String voice; // Optional, default to "ja-JP-Standard-A"
    private String speed; // Optional, default to "normal"
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
