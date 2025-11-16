package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Sentence Analysis
 * User provides a Japanese sentence and their JLPT level
 */
public class SentenceAnalysisRequest {

    @NotBlank(message = "Sentence is required")
    @Size(min = 1, max = 100, message = "Sentence must be between 1 and 100 characters")
    private String sentence; // Japanese sentence to analyze (15-30 characters recommended)

    @NotBlank(message = "Level is required")
    private String level; // JLPT level: N5, N4, N3, N2, N1

    public SentenceAnalysisRequest() {}

    public SentenceAnalysisRequest(String sentence, String level) {
        this.sentence = sentence;
        this.level = level;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isValidLevel() {
        if (level == null || level.isEmpty()) {
            return false;
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
        return "SentenceAnalysisRequest{" +
                "sentence='" + sentence + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
}

