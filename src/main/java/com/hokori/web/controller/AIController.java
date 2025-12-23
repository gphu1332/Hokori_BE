package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.TranslationRequest;
import com.hokori.web.dto.SentimentAnalysisRequest;
import com.hokori.web.dto.ContentGenerationRequest;
import com.hokori.web.dto.SpeechToTextRequest;
import com.hokori.web.dto.TextToSpeechRequest;
import com.hokori.web.dto.KaiwaPracticeRequest;
import com.hokori.web.dto.SentenceAnalysisRequest;
import com.hokori.web.dto.SentenceAnalysisResponse;
import com.hokori.web.dto.ConversationStartRequest;
import com.hokori.web.dto.ConversationRespondRequest;
import com.hokori.web.dto.ConversationEndRequest;
import com.hokori.web.service.AIService;
import com.hokori.web.service.SpeakingPracticeService;
import com.hokori.web.service.KaiwaSentenceService;
import com.hokori.web.service.SentenceAnalysisService;
import com.hokori.web.service.AIPackageService;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.ConversationPracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
    
    @Autowired(required = false)
    private SentenceAnalysisService sentenceAnalysisService;
    
    @Autowired(required = false)
    private AIPackageService aiPackageService;
    
    @Autowired(required = false)
    private CurrentUserService currentUserService;
    
    @Autowired(required = false)
    private ConversationPracticeService conversationPracticeService;

    @PostMapping("/translate")
    @Operation(
        summary = "Translate text",
        description = "Translate text using Google Cloud Translation API",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Text to translate and language settings",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TranslationRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"text\": \"私は日本語を勉強しています\",\n" +
                            "  \"sourceLanguage\": \"ja\",\n" +
                            "  \"targetLanguage\": \"vi\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Translation completed successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Translation completed\",\n" +
                                "  \"data\": {\n" +
                                "    \"translatedText\": \"Tôi đang học tiếng Nhật\",\n" +
                                "    \"sourceLanguage\": \"ja\",\n" +
                                "    \"targetLanguage\": \"vi\"\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
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
    @Operation(
        summary = "Analyze text sentiment",
        description = "Analyze text sentiment using Google Cloud Natural Language API",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Text to analyze for sentiment",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SentimentAnalysisRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"text\": \"日本語の勉強は楽しいです！\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Sentiment analysis completed successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Sentiment analysis completed\",\n" +
                                "  \"data\": {\n" +
                                "    \"sentiment\": \"POSITIVE\",\n" +
                                "    \"score\": 0.8,\n" +
                                "    \"magnitude\": 0.9\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
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
    @Operation(
        summary = "Generate content",
        description = "Generate content using Google Cloud AI",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Prompt and content type for generation",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContentGenerationRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"prompt\": \"Create a lesson about Japanese particles は and が\",\n" +
                            "  \"contentType\": \"lesson\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Content generated successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Content generated successfully\",\n" +
                                "  \"data\": {\n" +
                                "    \"content\": \"Lesson content here...\",\n" +
                                "    \"contentType\": \"lesson\"\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
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
    @Operation(
        summary = "Speech to text",
        description = "Convert speech to text using Google Cloud Speech-to-Text API. Optimized for Vietnamese users learning Japanese.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Base64 encoded audio data and language settings",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SpeechToTextRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"audioData\": \"UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=\",\n" +
                            "  \"language\": \"ja-JP\",\n" +
                            "  \"audioFormat\": \"wav\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Speech transcribed successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Speech transcribed successfully\",\n" +
                                "  \"data\": {\n" +
                                "    \"transcript\": \"私は日本語を勉強しています\",\n" +
                                "    \"confidence\": 0.95,\n" +
                                "    \"language\": \"ja-JP\"\n" +
                                "  },\n" +
                                "  \"timestamp\": \"2025-01-16T10:14:04.084Z\"\n" +
                                "}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Error response",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Error Response",
                        value = "{\n" +
                                "  \"success\": false,\n" +
                                "  \"message\": \"Invalid audio format. Valid formats: wav, mp3, flac, ogg, webm\",\n" +
                                "  \"data\": null\n" +
                                "}"
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> speechToText(
            @Valid @RequestBody SpeechToTextRequest request) {
        logger.info("Speech-to-text request: language={}, audioFormat={}, audioDataLength={}", 
            request.getLanguage(), request.getAudioFormat(),
            request.getAudioData() != null ? request.getAudioData().length() : 0);
        
        try {
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg, webm"));
            }
            
            Map<String, Object> transcriptionResult = aiService.speechToText(
                request.getAudioData(), 
                request.getLanguage(),
                request.getAudioFormat()
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
    @Operation(
        summary = "Text to speech",
        description = "Convert text to speech using Google Cloud Text-to-Speech API",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Text and voice settings for speech synthesis",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TextToSpeechRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"text\": \"こんにちは、私は日本語を勉強しています\",\n" +
                            "  \"voice\": \"ja-JP-Standard-A\",\n" +
                            "  \"speed\": \"normal\",\n" +
                            "  \"audioFormat\": \"mp3\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Text converted to speech successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Text converted to speech successfully\",\n" +
                                "  \"data\": {\n" +
                                "    \"audioData\": \"UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=\",\n" +
                                "    \"audioFormat\": \"mp3\",\n" +
                                "    \"audioSize\": 12345\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
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
    @Operation(
        summary = "Kaiwa practice",
        description = "Practice Japanese conversation by comparing pronunciation with target text. " +
                "Business Rules: " +
                "1) Audio limits: Max 1.3MB base64 (~1MB decoded, ~60 seconds duration). " +
                "2) Target text: Max 5000 characters (Japanese text to practice). " +
                "3) Transcript: Auto-generated from audio, typically 150-200 characters for 60 seconds. " +
                "4) Validation: Audio is validated for size (1MB decoded), duration (60 seconds), and transcript length (5000 chars max). " +
                "5) Google Cloud: Synchronous API supports max 60s; audio > 60s will be rejected. " +
                "6) Quota: Uses 1 request from unified AI quota pool per request.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Target text and user's recorded audio for practice. " +
                    "Audio must be base64 encoded, max 1.3MB base64 string (~1MB decoded = ~60 seconds). " +
                    "Target text max 5000 characters. " +
                    "Note: Google Cloud synchronous API supports max 60s; audio > 60s will be rejected.",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KaiwaPracticeRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"targetText\": \"こんにちは、私は日本語を勉強しています\",\n" +
                            "  \"audioData\": \"UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=\",\n" +
                            "  \"level\": \"N5\",\n" +
                            "  \"language\": \"ja-JP\",\n" +
                            "  \"audioFormat\": \"wav\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Kaiwa practice completed successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Kaiwa practice completed\",\n" +
                                "  \"data\": {\n" +
                                "    \"overallScore\": 85,\n" +
                                "    \"accuracyScore\": 90,\n" +
                                "    \"pronunciationScore\": 80,\n" +
                                "    \"feedback\": \"Good pronunciation!\"\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> practiceKaiwa(
            @Valid @RequestBody KaiwaPracticeRequest request) {
        logger.info("Kaiwa practice request: targetTextLength={}, audioDataLength={}, language={}, level={}", 
            request.getTargetText() != null ? request.getTargetText().length() : 0,
            request.getAudioData() != null ? request.getAudioData().length() : 0,
            request.getLanguage(),
            request.getLevel());
        
        try {
            // Check if user has permission to use AI features
            // MODERATOR: unlimited access
            // Other users: free tier or purchased package
            if (currentUserService != null && aiPackageService != null) {
                Long userId = currentUserService.getUserIdOrThrow();
                try {
                    if (!aiPackageService.canUseAIService(userId, com.hokori.web.Enum.AIServiceType.KAIWA)) {
                        // Get quota info to provide better error message
                        var quotaResponse = aiPackageService.getMyQuota(userId);
                        String errorMessage = "You have used all available AI requests. ";
                        if (quotaResponse.getRemainingRequests() != null && quotaResponse.getRemainingRequests() == 0) {
                            errorMessage += String.format("Used %d/%d requests. ", 
                                quotaResponse.getUsedRequests(), quotaResponse.getTotalRequests());
                        }
                        errorMessage += "Please purchase an AI package to continue using this feature.";
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(errorMessage));
                    }
                } catch (org.springframework.web.server.ResponseStatusException e) {
                    // Re-throw with better message
                    return ResponseEntity.status(e.getStatusCode())
                            .body(ApiResponse.error(e.getReason()));
                }
            }
            
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg, webm"));
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
                request.getLevel(),
                request.getAudioFormat()
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

    @PostMapping("/sentence-analysis")
    @Operation(
        summary = "Analyze Japanese sentence", 
        description = "Analyze Japanese sentence (max 200 characters) for notable vocabulary and grammar patterns using AI. Focuses on words and grammar worth learning at user's JLPT level. Designed for Vietnamese users learning Japanese. All explanations are in Vietnamese. Response is structured separately for vocabulary and grammar to make it easy to display in UI.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Japanese sentence and user's JLPT level",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SentenceAnalysisRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"sentence\": \"私は日本語を勉強しています\",\n" +
                            "  \"level\": \"N5\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Analysis completed successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Sentence analysis completed\",\n" +
                                "  \"data\": {\n" +
                                "    \"sentence\": \"私は日本語を勉強しています\",\n" +
                                "    \"level\": \"N5\",\n" +
                                "    \"vocabulary\": [\n" +
                                "      {\n" +
                                "        \"word\": \"私\",\n" +
                                "        \"reading\": \"わたし\",\n" +
                                "        \"meaningVi\": \"tôi\",\n" +
                                "        \"jlptLevel\": \"N5\",\n" +
                                "        \"importance\": \"high\",\n" +
                                "        \"kanjiDetails\": {\n" +
                                "          \"radical\": \"禾\",\n" +
                                "          \"strokeCount\": 7,\n" +
                                "          \"onyomi\": \"シ\",\n" +
                                "          \"kunyomi\": \"わたし\",\n" +
                                "          \"relatedWords\": [\"私的\", \"私立\"]\n" +
                                "        }\n" +
                                "      },\n" +
                                "      {\n" +
                                "        \"word\": \"日本語\",\n" +
                                "        \"reading\": \"にほんご\",\n" +
                                "        \"meaningVi\": \"tiếng Nhật\",\n" +
                                "        \"jlptLevel\": \"N5\",\n" +
                                "        \"importance\": \"high\"\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"grammar\": [\n" +
                                "      {\n" +
                                "        \"pattern\": \"を + verb\",\n" +
                                "        \"jlptLevel\": \"N5\",\n" +
                                "        \"explanationVi\": \"Trợ từ を được dùng để đánh dấu tân ngữ trực tiếp\",\n" +
                                "        \"example\": \"本を読みます\",\n" +
                                "        \"notes\": \"Lưu ý: Không nhầm với は (chủ đề)\"\n" +
                                "      },\n" +
                                "      {\n" +
                                "        \"pattern\": \"ています\",\n" +
                                "        \"jlptLevel\": \"N5\",\n" +
                                "        \"explanationVi\": \"Diễn tả hành động đang diễn ra hoặc trạng thái hiện tại\",\n" +
                                "        \"example\": \"勉強しています\",\n" +
                                "        \"notes\": \"Có thể dùng cho cả hành động và trạng thái\"\n" +
                                "      }\n" +
                                "    ]\n" +
                                "  }\n" +
                                "}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Error response",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Error Response",
                        value = "{\n" +
                                "  \"success\": false,\n" +
                                "  \"message\": \"Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1\",\n" +
                                "  \"data\": null\n" +
                                "}"
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<SentenceAnalysisResponse>> analyzeSentence(
            @Valid @RequestBody SentenceAnalysisRequest request) {
        logger.info("Sentence analysis request: sentenceLength={}, level={}",
            request.getSentence() != null ? request.getSentence().length() : 0,
            request.getLevel());

        try {
            if (!request.isValidLevel()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1"));
            }

            if (sentenceAnalysisService == null) {
                return ResponseEntity.ok(ApiResponse.error("Sentence analysis service is not available"));
            }

            SentenceAnalysisResponse analysisResult = sentenceAnalysisService.analyzeSentence(
                request.getSentence(),
                request.getLevel()
            );

            logger.debug("Sentence analysis successful: vocabularyCount={}, grammarCount={}",
                analysisResult.getVocabulary() != null ? analysisResult.getVocabulary().size() : 0,
                analysisResult.getGrammar() != null ? analysisResult.getGrammar().size() : 0);

            return ResponseEntity.ok(ApiResponse.success("Sentence analysis completed", analysisResult));
        } catch (Exception e) {
            logger.error("Sentence analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error("Sentence analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/sentence-examples/{level}")
    @Operation(
        summary = "Get example sentences for sentence analysis practice",
        description = "Get list of example Japanese sentences suitable for vocabulary and grammar analysis (NOT conversation practice). These sentences are designed for learning vocabulary and grammar patterns, not for speaking practice."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSentenceExamples(
            @PathVariable String level) {
        logger.info("Getting sentence examples for level: {}", level);
        
        if (sentenceAnalysisService == null) {
            return ResponseEntity.ok(ApiResponse.error("Sentence analysis service is not available"));
        }
        
        try {
            // Validate level
            if (!isValidLevel(level)) {
                return ResponseEntity.ok(ApiResponse.error("Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1"));
            }

            List<Map<String, Object>> sentences = sentenceAnalysisService.getExampleSentences(level);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("level", level.toUpperCase());
            result.put("sentences", sentences);
            result.put("count", sentences.size());
            
            return ResponseEntity.ok(ApiResponse.success("Example sentences for sentence analysis - " + level, result));
        } catch (Exception e) {
            logger.error("Failed to get sentence examples", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get sentence examples: " + e.getMessage()));
        }
    }
    
    @GetMapping("/sentence-examples/{level}/random")
    @Operation(
        summary = "Get random example sentence for sentence analysis practice",
        description = "Get a random example Japanese sentence suitable for vocabulary and grammar analysis (NOT conversation practice)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRandomSentenceExample(
            @PathVariable String level) {
        logger.info("Getting random sentence example for level: {}", level);
        
        if (sentenceAnalysisService == null) {
            return ResponseEntity.ok(ApiResponse.error("Sentence analysis service is not available"));
        }
        
        try {
            // Validate level
            if (!isValidLevel(level)) {
                return ResponseEntity.ok(ApiResponse.error("Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1"));
            }

            Map<String, Object> sentence = sentenceAnalysisService.getRandomExampleSentence(level);
            
            if (sentence == null) {
                return ResponseEntity.ok(ApiResponse.error("No example sentences available for level " + level));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Random sentence example for " + level, sentence));
        } catch (Exception e) {
            logger.error("Failed to get random sentence example", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get random sentence example: " + e.getMessage()));
        }
    }

    /**
     * Helper method to validate JLPT level
     */
    private boolean isValidLevel(String level) {
        if (level == null || level.isEmpty()) {
            return false;
        }
        String upperLevel = level.toUpperCase();
        return upperLevel.equals("N5") || upperLevel.equals("N4") || 
               upperLevel.equals("N3") || upperLevel.equals("N2") || upperLevel.equals("N1");
    }

    // =================================================================================
    // CONVERSATION PRACTICE ENDPOINTS
    // =================================================================================

    @PostMapping("/conversation/start")
    @Operation(
        summary = "Start conversation practice session",
        description = "Start a new conversation practice session. AI will ask the first question based on scenario and level.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Level and scenario for conversation practice",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversationStartRequest.class),
                examples = @ExampleObject(
                    name = "Example Request",
                    value = "{\n" +
                            "  \"level\": \"N5\",\n" +
                            "  \"scenario\": \"restaurant\"\n" +
                            "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Conversation started successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"message\": \"Conversation started\",\n" +
                                "  \"data\": {\n" +
                                "    \"conversationId\": \"conv-abc123\",\n" +
                                "    \"level\": \"N5\",\n" +
                                "    \"scenario\": \"restaurant\",\n" +
                                "    \"aiQuestion\": \"こんにちは、いらっしゃいませ\",\n" +
                                "    \"aiQuestionVi\": \"Xin chào, chào mừng quý khách\",\n" +
                                "    \"audioUrl\": \"base64...\",\n" +
                                "    \"conversationHistory\": [...],\n" +
                                "    \"turnNumber\": 1,\n" +
                                "    \"maxTurns\": 7\n" +
                                "  }\n" +
                                "}"
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> startConversation(
            @Valid @RequestBody ConversationStartRequest request) {
        logger.info("Conversation start request: level={}, scenario={}", request.getLevel(), request.getScenario());
        
        try {
            // Check quota
            if (currentUserService != null && aiPackageService != null) {
                Long userId = currentUserService.getUserIdOrThrow();
                try {
                    if (!aiPackageService.canUseAIService(userId, com.hokori.web.Enum.AIServiceType.CONVERSATION)) {
                        // Get quota info to provide better error message
                        var quotaResponse = aiPackageService.getMyQuota(userId);
                        String errorMessage = "You have used all available AI requests. ";
                        if (quotaResponse.getRemainingRequests() != null && quotaResponse.getRemainingRequests() == 0) {
                            errorMessage += String.format("Used %d/%d requests. ", 
                                quotaResponse.getUsedRequests(), quotaResponse.getTotalRequests());
                        }
                        errorMessage += "Please purchase an AI package to continue using this feature.";
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(errorMessage));
                    }
                } catch (org.springframework.web.server.ResponseStatusException e) {
                    // Re-throw with better message
                    return ResponseEntity.status(e.getStatusCode())
                            .body(ApiResponse.error(e.getReason()));
                }
            }
            
            if (!request.isValidLevel()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1"));
            }
            
            if (conversationPracticeService == null) {
                return ResponseEntity.ok(ApiResponse.error("Conversation practice service is not available"));
            }
            
            Map<String, Object> result = conversationPracticeService.startConversation(
                request.getLevel(),
                request.getScenario()
            );
            
            // Deduct quota
            if (currentUserService != null && aiPackageService != null) {
                Long userId = currentUserService.getUserIdOrThrow();
                aiPackageService.useAIService(userId, com.hokori.web.Enum.AIServiceType.CONVERSATION, 1);
            }
            
            logger.debug("Conversation started successfully: conversationId={}", result.get("conversationId"));
            return ResponseEntity.ok(ApiResponse.success("Conversation started", result));
        } catch (Exception e) {
            logger.error("Conversation start failed", e);
            return ResponseEntity.ok(ApiResponse.error("Conversation start failed: " + e.getMessage()));
        }
    }

    @PostMapping("/conversation/respond")
    @Operation(
        summary = "Respond to conversation",
        description = "Respond to AI's question and get next question. FE maintains conversation history.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Conversation ID, history, and user audio",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversationRespondRequest.class)
            )
        )
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> respondToConversation(
            @Valid @RequestBody ConversationRespondRequest request) {
        logger.info("Conversation respond request: conversationId={}, historySize={}", 
            request.getConversationId(), 
            request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);
        
        try {
            if (!request.isValidAudioFormat()) {
                return ResponseEntity.ok(ApiResponse.error("Invalid audio format. Valid formats: wav, mp3, flac, ogg, webm"));
            }
            
            // Validate audio data is not empty
            if (!request.isValidAudioData()) {
                return ResponseEntity.ok(ApiResponse.error(
                    "Audio data is empty or too short. Please record your response before submitting. " +
                    "Make sure you have spoken something and the recording is complete."
                ));
            }
            
            if (conversationPracticeService == null) {
                return ResponseEntity.ok(ApiResponse.error("Conversation practice service is not available"));
            }
            
            String level = (request.getLevel() != null && !request.getLevel().isEmpty()) 
                ? request.getLevel() : "N5";
            String scenario = (request.getScenario() != null && !request.getScenario().isEmpty()) 
                ? request.getScenario() : "greeting";
            
            Map<String, Object> result = conversationPracticeService.respondToConversation(
                request.getConversationId(),
                request.getConversationHistory(),
                request.getAudioData(),
                request.getAudioFormat(),
                request.getLanguage(),
                level,
                scenario
            );
            
            logger.debug("Conversation response processed: turnNumber={}", result.get("turnNumber"));
            return ResponseEntity.ok(ApiResponse.success("Conversation response processed", result));
        } catch (Exception e) {
            logger.error("Conversation respond failed", e);
            return ResponseEntity.ok(ApiResponse.error("Conversation respond failed: " + e.getMessage()));
        }
    }

    @PostMapping("/conversation/end")
    @Operation(
        summary = "End conversation and get evaluation",
        description = "End conversation practice session and receive AI evaluation and feedback.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Conversation ID and final history",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversationEndRequest.class)
            )
        )
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> endConversation(
            @Valid @RequestBody ConversationEndRequest request) {
        logger.info("Conversation end request: conversationId={}, historySize={}", 
            request.getConversationId(),
            request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);
        
        try {
            if (conversationPracticeService == null) {
                return ResponseEntity.ok(ApiResponse.error("Conversation practice service is not available"));
            }
            
            String level = (request.getLevel() != null && !request.getLevel().isEmpty()) 
                ? request.getLevel() : "N5";
            String scenario = (request.getScenario() != null && !request.getScenario().isEmpty()) 
                ? request.getScenario() : "greeting";
            
            Map<String, Object> result = conversationPracticeService.endConversation(
                request.getConversationId(),
                request.getConversationHistory(),
                level,
                scenario
            );
            
            logger.debug("Conversation ended successfully: conversationId={}", request.getConversationId());
            return ResponseEntity.ok(ApiResponse.success("Conversation ended", result));
        } catch (Exception e) {
            logger.error("Conversation end failed", e);
            return ResponseEntity.ok(ApiResponse.error("Conversation end failed: " + e.getMessage()));
        }
    }
}
