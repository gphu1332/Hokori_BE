package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.TranslationRequest;
import com.hokori.web.dto.SentimentAnalysisRequest;
import com.hokori.web.dto.ContentGenerationRequest;
import com.hokori.web.dto.SpeechToTextRequest;
import com.hokori.web.dto.TextToSpeechRequest;
import com.hokori.web.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Services", description = "Google Cloud AI integration endpoints")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/translate")
    @Operation(summary = "Translate text", description = "Translate text using Google Cloud Translation API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> translateText(
            @Valid @RequestBody TranslationRequest request) {
        try {
            Map<String, Object> translationResult = aiService.translateText(
                request.getText(), 
                request.getSourceLanguage(), 
                request.getTargetLanguage()
            );
            return ResponseEntity.ok(ApiResponse.success("Translation completed", translationResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Translation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze-sentiment")
    @Operation(summary = "Analyze text sentiment", description = "Analyze text sentiment using Google Cloud Natural Language API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSentiment(
            @Valid @RequestBody SentimentAnalysisRequest request) {
        try {
            Map<String, Object> sentimentResult = aiService.analyzeSentiment(request.getText());
            return ResponseEntity.ok(ApiResponse.success("Sentiment analysis completed", sentimentResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Sentiment analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/generate-content")
    @Operation(summary = "Generate content", description = "Generate content using Google Cloud AI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContent(
            @Valid @RequestBody ContentGenerationRequest request) {
        try {
            if (!request.isValidContentType()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid content type. Valid types: lesson, exercise, explanation, general"));
            }
            
            Map<String, Object> contentResult = aiService.generateContent(request.getPrompt(), request.getContentType());
            return ResponseEntity.ok(ApiResponse.success("Content generated successfully", contentResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Content generation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/speech-to-text")
    @Operation(summary = "Speech to text", description = "Convert speech to text using Google Cloud Speech-to-Text API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> speechToText(
            @Valid @RequestBody SpeechToTextRequest request) {
        try {
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg"));
            }
            
            Map<String, Object> transcriptionResult = aiService.speechToText(
                request.getAudioData(), 
                request.getLanguage()
            );
            return ResponseEntity.ok(ApiResponse.success("Speech transcribed successfully", transcriptionResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Speech transcription failed: " + e.getMessage()));
        }
    }

    @PostMapping("/text-to-speech")
    @Operation(summary = "Text to speech", description = "Convert text to speech using Google Cloud Text-to-Speech API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> textToSpeech(
            @Valid @RequestBody TextToSpeechRequest request) {
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
            return ResponseEntity.ok(ApiResponse.success("Text converted to speech successfully", speechResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Text-to-speech conversion failed: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "AI service health check", description = "Check if AI services are available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthStatus = aiService.getHealthStatus();
        return ResponseEntity.ok(ApiResponse.success("AI services health check", healthStatus));
    }
}
