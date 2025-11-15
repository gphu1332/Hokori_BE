package com.hokori.web.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to format and transform AI service responses for Vietnamese users learning Japanese
 * This service applies business rules specific to Vietnamese users
 */
@Component
public class AIResponseFormatter {
    
    /**
     * Format translation response for Vietnamese users
     * Adds metadata and transforms data for better UX
     */
    public Map<String, Object> formatTranslationResponse(Map<String, Object> rawResponse) {
        Map<String, Object> formatted = new HashMap<>(rawResponse);
        
        // Add metadata for Vietnamese users
        Map<String, Object> meta = new HashMap<>();
        meta.put("targetAudience", "Vietnamese users learning Japanese");
        meta.put("defaultSourceLanguage", "ja");
        meta.put("defaultTargetLanguage", "vi");
        
        // Add language display names
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("ja", "Tiếng Nhật");
        languageNames.put("vi", "Tiếng Việt");
        languageNames.put("en", "Tiếng Anh");
        meta.put("languageNames", languageNames);
        
        formatted.put("meta", meta);
        
        return formatted;
    }
    
    /**
     * Format sentiment analysis response for Vietnamese users
     * Adds Vietnamese-friendly labels and explanations
     */
    public Map<String, Object> formatSentimentResponse(Map<String, Object> rawResponse) {
        Map<String, Object> formatted = new HashMap<>(rawResponse);
        
        // Add Vietnamese labels
        String sentiment = (String) rawResponse.get("sentiment");
        Map<String, String> sentimentLabels = new HashMap<>();
        sentimentLabels.put("POSITIVE", "Tích cực");
        sentimentLabels.put("NEGATIVE", "Tiêu cực");
        sentimentLabels.put("NEUTRAL", "Trung tính");
        
        formatted.put("sentimentLabelVi", sentimentLabels.getOrDefault(sentiment, sentiment));
        
        // Add explanation in Vietnamese
        Float score = (Float) rawResponse.get("score");
        String explanation = getSentimentExplanationVi(score);
        formatted.put("explanationVi", explanation);
        
        // Add metadata
        Map<String, Object> meta = new HashMap<>();
        meta.put("targetAudience", "Vietnamese users learning Japanese");
        meta.put("analysisType", "sentiment");
        formatted.put("meta", meta);
        
        return formatted;
    }
    
    /**
     * Format text-to-speech response for Vietnamese users
     * Adds metadata about Japanese pronunciation
     */
    public Map<String, Object> formatTextToSpeechResponse(Map<String, Object> rawResponse) {
        Map<String, Object> formatted = new HashMap<>(rawResponse);
        
        // Add metadata
        Map<String, Object> meta = new HashMap<>();
        meta.put("targetAudience", "Vietnamese users learning Japanese");
        meta.put("defaultVoice", "ja-JP-Standard-A");
        meta.put("voiceDescription", "Giọng nữ tiếng Nhật chuẩn");
        meta.put("purpose", "Pronunciation practice for Vietnamese learners");
        formatted.put("meta", meta);
        
        return formatted;
    }
    
    /**
     * Format speech-to-text response for Vietnamese users
     * Adds feedback and suggestions
     */
    public Map<String, Object> formatSpeechToTextResponse(Map<String, Object> rawResponse) {
        Map<String, Object> formatted = new HashMap<>(rawResponse);
        
        // Add feedback in Vietnamese
        Double confidence = (Double) rawResponse.get("confidence");
        if (confidence != null) {
            String feedbackVi = getPronunciationFeedbackVi(confidence);
            formatted.put("feedbackVi", feedbackVi);
        }
        
        // Add metadata
        Map<String, Object> meta = new HashMap<>();
        meta.put("targetAudience", "Vietnamese users learning Japanese");
        meta.put("defaultLanguage", "ja-JP");
        meta.put("purpose", "Pronunciation correction for Vietnamese learners");
        formatted.put("meta", meta);
        
        return formatted;
    }
    
    /**
     * Get sentiment explanation in Vietnamese
     */
    private String getSentimentExplanationVi(Float score) {
        if (score == null) {
            return "Không thể phân tích cảm xúc";
        }
        
        if (score >= 0.25) {
            return String.format("Văn bản có cảm xúc tích cực (điểm số: %.2f). Nội dung này thể hiện cảm xúc vui vẻ, lạc quan.", score);
        } else if (score <= -0.25) {
            return String.format("Văn bản có cảm xúc tiêu cực (điểm số: %.2f). Nội dung này thể hiện cảm xúc buồn bã, lo lắng.", score);
        } else {
            return String.format("Văn bản có cảm xúc trung tính (điểm số: %.2f). Nội dung này không thể hiện cảm xúc rõ ràng.", score);
        }
    }
    
    /**
     * Get pronunciation feedback in Vietnamese
     */
    private String getPronunciationFeedbackVi(Double confidence) {
        if (confidence == null) {
            return "Không thể đánh giá phát âm";
        }
        
        if (confidence >= 0.9) {
            return String.format("Phát âm xuất sắc! (độ chính xác: %.0f%%)", confidence * 100);
        } else if (confidence >= 0.7) {
            return String.format("Phát âm tốt! (độ chính xác: %.0f%%)", confidence * 100);
        } else if (confidence >= 0.5) {
            return String.format("Phát âm cần cải thiện (độ chính xác: %.0f%%)", confidence * 100);
        } else {
            return String.format("Phát âm cần luyện tập nhiều hơn (độ chính xác: %.0f%%)", confidence * 100);
        }
    }
    
    /**
     * Get default language settings for Vietnamese users
     */
    public Map<String, String> getDefaultLanguageSettings() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("nativeLanguage", "vi");
        defaults.put("learningLanguage", "ja");
        defaults.put("translationSource", "ja");
        defaults.put("translationTarget", "vi");
        defaults.put("textToSpeechVoice", "ja-JP-Standard-A");
        defaults.put("speechToTextLanguage", "ja-JP");
        return defaults;
    }
}

