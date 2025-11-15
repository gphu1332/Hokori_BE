package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.TranslationRequest;
import com.hokori.web.dto.SentimentAnalysisRequest;
import com.hokori.web.dto.ContentGenerationRequest;
import com.hokori.web.dto.SpeechToTextRequest;
import com.hokori.web.dto.TextToSpeechRequest;
import com.hokori.web.dto.KaiwaPracticeRequest;
import com.hokori.web.service.AIService;
import com.hokori.web.service.SpeakingPracticeService;
import com.hokori.web.service.KaiwaSentenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Services", description = "Google Cloud AI integration endpoints")
@CrossOrigin(origins = "*")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIService aiService;
    
    @Autowired(required = false)
    private SpeakingPracticeService speakingPracticeService;
    
    @Autowired(required = false)
    private KaiwaSentenceService kaiwaSentenceService;
    
    @Autowired(required = false)
    private com.hokori.web.service.AIResponseFormatter responseFormatter;

    @PostMapping("/translate")
    @Operation(summary = "Translate text", description = "Translate text using Google Cloud Translation API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> translateText(
            @Valid @RequestBody TranslationRequest request) {
        logger.info("Translation request: source={}, target={}, textLength={}", 
            request.getSourceLanguage(), request.getTargetLanguage(), 
            request.getText() != null ? request.getText().length() : 0);
        
        try {
            Map<String, Object> translationResult = aiService.translateText(
                request.getText(), 
                request.getSourceLanguage(), 
                request.getTargetLanguage()
            );
            logger.debug("Translation successful: {}", translationResult.get("translatedText"));
            return ResponseEntity.ok(ApiResponse.success("Translation completed", translationResult));
        } catch (Exception e) {
            logger.error("Translation failed", e);
            return ResponseEntity.ok(ApiResponse.error("Translation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze-sentiment")
    @Operation(summary = "Analyze text sentiment", description = "Analyze text sentiment using Google Cloud Natural Language API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSentiment(
            @Valid @RequestBody SentimentAnalysisRequest request) {
        logger.info("Sentiment analysis request: textLength={}", 
            request.getText() != null ? request.getText().length() : 0);
        
        try {
            Map<String, Object> sentimentResult = aiService.analyzeSentiment(request.getText());
            logger.debug("Sentiment analysis successful: sentiment={}, score={}", 
                sentimentResult.get("sentiment"), sentimentResult.get("score"));
            return ResponseEntity.ok(ApiResponse.success("Sentiment analysis completed", sentimentResult));
        } catch (Exception e) {
            logger.error("Sentiment analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error("Sentiment analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/generate-content")
    @Operation(summary = "Generate content", description = "Generate content using Google Cloud AI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContent(
            @Valid @RequestBody ContentGenerationRequest request) {
        logger.info("Content generation request: contentType={}, promptLength={}", 
            request.getContentType(),
            request.getPrompt() != null ? request.getPrompt().length() : 0);
        
        try {
            if (!request.isValidContentType()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid content type. Valid types: lesson, exercise, explanation, general"));
            }
            
            Map<String, Object> contentResult = aiService.generateContent(request.getPrompt(), request.getContentType());
            logger.debug("Content generation successful: contentType={}", request.getContentType());
            return ResponseEntity.ok(ApiResponse.success("Content generated successfully", contentResult));
        } catch (Exception e) {
            logger.error("Content generation failed", e);
            return ResponseEntity.ok(ApiResponse.error("Content generation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/speech-to-text")
    @Operation(summary = "Speech to text", description = "Convert speech to text using Google Cloud Speech-to-Text API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> speechToText(
            @Valid @RequestBody SpeechToTextRequest request) {
        logger.info("Speech-to-text request: language={}, audioFormat={}, audioDataLength={}", 
            request.getLanguage(), request.getAudioFormat(),
            request.getAudioData() != null ? request.getAudioData().length() : 0);
        
        try {
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg"));
            }
            
            Map<String, Object> transcriptionResult = aiService.speechToText(
                request.getAudioData(), 
                request.getLanguage()
            );
            logger.debug("Speech-to-text successful: transcript={}, confidence={}", 
                transcriptionResult.get("transcript"), transcriptionResult.get("confidence"));
            return ResponseEntity.ok(ApiResponse.success("Speech transcribed successfully", transcriptionResult));
        } catch (Exception e) {
            logger.error("Speech transcription failed", e);
            return ResponseEntity.ok(ApiResponse.error("Speech transcription failed: " + e.getMessage()));
        }
    }

    @PostMapping("/text-to-speech")
    @Operation(summary = "Text to speech", description = "Convert text to speech using Google Cloud Text-to-Speech API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> textToSpeech(
            @Valid @RequestBody TextToSpeechRequest request) {
        logger.info("Text-to-speech request: voice={}, speed={}, textLength={}", 
            request.getVoice(), request.getSpeed(),
            request.getText() != null ? request.getText().length() : 0);
        
        try {
            if (!request.isValidSpeed()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid speed. Valid speeds: slow, normal, fast"));
            }
            
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: mp3, wav, ogg"));
            }
            
            Map<String, Object> speechResult = aiService.textToSpeech(
                request.getText(), 
                request.getVoice(), 
                request.getSpeed()
            );
            logger.debug("Text-to-speech successful: audioSize={}", speechResult.get("audioSize"));
            return ResponseEntity.ok(ApiResponse.success("Text converted to speech successfully", speechResult));
        } catch (Exception e) {
            logger.error("Text-to-speech conversion failed", e);
            return ResponseEntity.ok(ApiResponse.error("Text-to-speech conversion failed: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "AI service health check", description = "Check if AI services are available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthStatus = aiService.getHealthStatus();
        return ResponseEntity.ok(ApiResponse.success("AI services health check", healthStatus));
    }
    
    @PostMapping("/kaiwa-practice")
    @Operation(summary = "Kaiwa practice", description = "Practice Japanese conversation by comparing pronunciation with target text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> practiceKaiwa(
            @Valid @RequestBody KaiwaPracticeRequest request) {
        logger.info("Kaiwa practice request: targetTextLength={}, audioDataLength={}, language={}, level={}", 
            request.getTargetText() != null ? request.getTargetText().length() : 0,
            request.getAudioData() != null ? request.getAudioData().length() : 0,
            request.getLanguage(),
            request.getLevel());
        
        try {
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg"));
            }
            
            if (!request.isValidLevel()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1"));
            }
            
            if (speakingPracticeService == null) {
                return ResponseEntity.ok(ApiResponse.error("Speaking practice service is not available"));
            }
            
            Map<String, Object> practiceResult = speakingPracticeService.practiceKaiwa(
                request.getTargetText(),
                request.getAudioData(),
                request.getLanguage(),
                request.getLevel()
            );
            
            logger.debug("Kaiwa practice successful: overallScore={}, accuracyScore={}", 
                practiceResult.get("overallScore"), practiceResult.get("accuracyScore"));
            
            return ResponseEntity.ok(ApiResponse.success("Kaiwa practice completed", practiceResult));
        } catch (Exception e) {
            logger.error("Kaiwa practice failed", e);
            return ResponseEntity.ok(ApiResponse.error("Kaiwa practice failed: " + e.getMessage()));
        }
    }

    @GetMapping("/kaiwa-recommendations/{level}")
    @Operation(summary = "Get kaiwa practice recommendations by JLPT level", description = "Get recommended settings (speed, thresholds) for kaiwa practice based on JLPT level")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKaiwaRecommendations(
            @PathVariable String level) {
        logger.info("Getting kaiwa recommendations for level: {}", level);
        
        if (speakingPracticeService == null) {
            return ResponseEntity.ok(ApiResponse.error("Speaking practice service is not available"));
        }
        
        Map<String, Object> recommendations = new java.util.HashMap<>();
        recommendations.put("level", level);
        recommendations.put("recommendedSpeed", speakingPracticeService.getRecommendedSpeed(level));
        recommendations.put("recommendedSpeakingRate", speakingPracticeService.getRecommendedSpeakingRate(level));
        
        // Add level info
        Map<String, Object> levelInfo = new java.util.HashMap<>();
        levelInfo.put("level", level);
        levelInfo.put("description", "Recommended settings for " + level + " level practice");
        recommendations.put("levelInfo", levelInfo);
        
        return ResponseEntity.ok(ApiResponse.success("Kaiwa recommendations for " + level, recommendations));
    }
    
    @GetMapping("/kaiwa-sentences/{level}")
    @Operation(summary = "Get suggested sentences for kaiwa practice", description = "Get list of suggested Japanese sentences for practice based on JLPT level")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestedSentences(
            @PathVariable String level) {
        logger.info("Getting suggested sentences for level: {}", level);
        
        if (kaiwaSentenceService == null) {
            return ResponseEntity.ok(ApiResponse.error("Kaiwa sentence service is not available"));
        }
        
        List<Map<String, Object>> sentences = kaiwaSentenceService.getSuggestedSentences(level);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("level", level);
        result.put("sentences", sentences);
        result.put("count", sentences.size());
        
        return ResponseEntity.ok(ApiResponse.success("Suggested sentences for " + level, result));
    }
    
    @GetMapping("/kaiwa-sentences/{level}/random")
    @Operation(summary = "Get random sentence for kaiwa practice", description = "Get a random Japanese sentence for practice based on JLPT level")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRandomSentence(
            @PathVariable String level) {
        logger.info("Getting random sentence for level: {}", level);
        
        if (kaiwaSentenceService == null) {
            return ResponseEntity.ok(ApiResponse.error("Kaiwa sentence service is not available"));
        }
        
        Map<String, Object> sentence = kaiwaSentenceService.getRandomSentence(level);
        
        if (sentence == null) {
            return ResponseEntity.ok(ApiResponse.error("No sentences available for level " + level));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Random sentence for " + level, sentence));
    }

    @GetMapping("/defaults")
    @Operation(summary = "Get default settings for Vietnamese users", description = "Get default language and voice settings optimized for Vietnamese users learning Japanese")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDefaultSettings() {
        Map<String, Object> defaults = new java.util.HashMap<>();
        
        if (responseFormatter != null) {
            defaults.putAll(responseFormatter.getDefaultLanguageSettings());
        } else {
            // Fallback defaults
            defaults.put("nativeLanguage", "vi");
            defaults.put("learningLanguage", "ja");
            defaults.put("translationSource", "ja");
            defaults.put("translationTarget", "vi");
            defaults.put("textToSpeechVoice", "ja-JP-Standard-A");
            defaults.put("speechToTextLanguage", "ja-JP");
        }
        
        // Add business rules
        Map<String, Object> businessRules = new java.util.HashMap<>();
        businessRules.put("targetAudience", "Vietnamese users learning Japanese");
        businessRules.put("defaultTranslationDirection", "Japanese → Vietnamese");
        businessRules.put("supportedLanguages", java.util.Arrays.asList("ja", "vi", "en"));
        businessRules.put("recommendedVoices", java.util.Arrays.asList(
            "ja-JP-Standard-A", "ja-JP-Standard-B", "ja-JP-Standard-C", "ja-JP-Standard-D"
        ));
        defaults.put("businessRules", businessRules);
        
        return ResponseEntity.ok(ApiResponse.success("Default settings for Vietnamese users", defaults));
    }
}
