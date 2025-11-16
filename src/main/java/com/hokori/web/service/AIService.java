package com.hokori.web.service;

import com.google.cloud.language.v1.*;
import com.google.cloud.speech.v1.*;
import com.google.cloud.texttospeech.v1.*;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;
import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Google Cloud AI integration
 */
@Service
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    @Value("${google.cloud.project-id:hokori-web}")
    private String projectId;
    
    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;
    
    // AI Service Limits
    @Value("${ai.translation.max-length:5000}")
    private int translationMaxLength;
    
    @Value("${ai.sentiment.max-length:10000}")
    private int sentimentMaxLength;
    
    @Value("${ai.text-to-speech.max-length:5000}")
    private int textToSpeechMaxLength;
    
    @Value("${ai.speech-to-text.sample-rate:16000}")
    private int speechToTextSampleRate;
    
    // Text-to-Speech Speed Configuration
    @Value("${ai.text-to-speech.speed.slow:0.75}")
    private double textToSpeechSpeedSlow;
    
    @Value("${ai.text-to-speech.speed.normal:1.0}")
    private double textToSpeechSpeedNormal;
    
    @Value("${ai.text-to-speech.speed.fast:1.5}")
    private double textToSpeechSpeedFast;
    
    // Sentiment Analysis Thresholds
    @Value("${ai.feedback.threshold.sentiment.positive:0.25}")
    private float sentimentThresholdPositive;
    
    @Value("${ai.feedback.threshold.sentiment.negative:-0.25}")
    private float sentimentThresholdNegative;
    
    @Autowired(required = false)
    private Translate translateClient;
    
    @Autowired(required = false)
    private LanguageServiceClient languageServiceClient;
    
    @Autowired(required = false)
    private SpeechClient speechClient;
    
    @Autowired(required = false)
    private TextToSpeechClient textToSpeechClient;
    
    @Autowired(required = false)
    private AIResponseFormatter responseFormatter;
    
    /**
     * Translate text using Google Cloud Translation API
     */
    public Map<String, Object> translateText(String text, String sourceLanguage, String targetLanguage) {
        if (!googleCloudEnabled || translateClient == null) {
            throw new AIServiceException("Translation", 
                "Google Cloud Translation API is not enabled or not configured. Please enable it in application properties.",
                "TRANSLATION_SERVICE_DISABLED");
        }
        
        // Input validation
        if (!StringUtils.hasText(text)) {
            throw new AIServiceException("Translation", "Text cannot be empty", "INVALID_INPUT");
        }
        
        if (text.length() > translationMaxLength) {
            throw new AIServiceException("Translation", "Text exceeds maximum length of " + translationMaxLength + " characters", "INVALID_INPUT");
        }
        
        try {
            logger.debug("Translating text: length={}, source={}, target={}", text.length(), sourceLanguage, targetLanguage);
            // Default to Vietnamese for Vietnamese users learning Japanese
            String targetLang = targetLanguage != null && !targetLanguage.isEmpty() ? targetLanguage : "vi";
            
            // Auto-detect source language if not specified
            Translation translation;
            if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
                translation = translateClient.translate(
                    text,
                    Translate.TranslateOption.sourceLanguage(sourceLanguage),
                    Translate.TranslateOption.targetLanguage(targetLang)
                );
            } else {
                translation = translateClient.translate(
                    text,
                    Translate.TranslateOption.targetLanguage(targetLang)
                );
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("originalText", text);
            result.put("translatedText", translation.getTranslatedText());
            result.put("detectedSourceLanguage", translation.getSourceLanguage());
            result.put("targetLanguage", targetLang);
            result.put("confidence", translation.getTranslatedText().length() > 0 ? 1.0 : 0.0);
            
            // Format response for Vietnamese users
            if (responseFormatter != null) {
                result = responseFormatter.formatTranslationResponse(result);
            }
            
            logger.debug("Translation successful: detectedLanguage={}", result.get("detectedSourceLanguage"));
            return result;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Translation API error", e);
            throw new AIServiceException("Translation", "Translation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyze text sentiment using Google Cloud Natural Language API
     */
    public Map<String, Object> analyzeSentiment(String text) {
        if (!googleCloudEnabled || languageServiceClient == null) {
            throw new AIServiceException("Sentiment Analysis", 
                "Google Cloud Natural Language API is not enabled or not configured. Please enable it in application properties.",
                "SENTIMENT_SERVICE_DISABLED");
        }
        
        // Input validation
        if (!StringUtils.hasText(text)) {
            throw new AIServiceException("Sentiment Analysis", "Text cannot be empty", "INVALID_INPUT");
        }
        
        if (text.length() > sentimentMaxLength) {
            throw new AIServiceException("Sentiment Analysis", "Text exceeds maximum length of " + sentimentMaxLength + " characters", "INVALID_INPUT");
        }
        
        try {
            logger.debug("Analyzing sentiment: textLength={}", text.length());
            Document doc = Document.newBuilder()
                .setContent(text)
                .setType(Document.Type.PLAIN_TEXT)
                .build();
            
            AnalyzeSentimentResponse response = languageServiceClient.analyzeSentiment(
                AnalyzeSentimentRequest.newBuilder()
                    .setDocument(doc)
                    .build()
            );
            
            Sentiment sentiment = response.getDocumentSentiment();
            
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            result.put("score", sentiment.getScore());
            result.put("magnitude", sentiment.getMagnitude());
            result.put("sentiment", getSentimentLabel(sentiment.getScore()));
            
            // Sentence-level sentiment
            @SuppressWarnings("unchecked")
            Map<String, Object>[] sentences = new Map[response.getSentencesCount()];
            for (int i = 0; i < response.getSentencesCount(); i++) {
                Map<String, Object> sentenceSentiment = new HashMap<>();
                Sentiment sentenceSent = response.getSentences(i).getSentiment();
                sentenceSentiment.put("text", response.getSentences(i).getText().getContent());
                sentenceSentiment.put("score", sentenceSent.getScore());
                sentenceSentiment.put("magnitude", sentenceSent.getMagnitude());
                sentences[i] = sentenceSentiment;
            }
            result.put("sentences", sentences);
            
            // Format response for Vietnamese users
            if (responseFormatter != null) {
                result = responseFormatter.formatSentimentResponse(result);
            }
            
            logger.debug("Sentiment analysis successful: sentiment={}, score={}", result.get("sentiment"), result.get("score"));
            return result;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Sentiment analysis API error", e);
            throw new AIServiceException("Sentiment Analysis", "Sentiment analysis failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate content using AI (using Natural Language API for now, can be extended with Vertex AI)
     */
    public Map<String, Object> generateContent(String prompt, String contentType) {
        if (!googleCloudEnabled) {
            throw new AIServiceException("Content Generation", 
                "Google Cloud AI is not enabled. Please enable it in application properties.",
                "CONTENT_GENERATION_SERVICE_DISABLED");
        }
        
        // Input validation
        if (!StringUtils.hasText(prompt)) {
            throw new AIServiceException("Content Generation", "Prompt cannot be empty", "INVALID_INPUT");
        }
        
        // For now, we'll use sentiment analysis as a placeholder
        // In the future, this can be extended with Vertex AI for content generation
        try {
            logger.debug("Generating content: contentType={}, promptLength={}", contentType, prompt.length());
            Map<String, Object> sentimentResult = analyzeSentiment(prompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("prompt", prompt);
            result.put("contentType", contentType);
            result.put("generatedContent", "Content generation feature is under development. Currently analyzing sentiment: " + sentimentResult.get("sentiment"));
            result.put("note", "This endpoint will use Vertex AI for content generation in future updates.");
            
            logger.debug("Content generation successful: contentType={}", contentType);
            return result;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Content generation API error", e);
            throw new AIServiceException("Content Generation", "Content generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert speech to text using Google Cloud Speech-to-Text API
     * @param audioData Base64 encoded audio data
     * @param language Language code (e.g., "ja-JP")
     * @param audioFormat Audio format (e.g., "wav", "mp3", "ogg", "webm"). If null or empty, defaults to LINEAR16.
     */
    public Map<String, Object> speechToText(String audioData, String language) {
        return speechToText(audioData, language, null);
    }
    
    /**
     * Convert speech to text using Google Cloud Speech-to-Text API
     * @param audioData Base64 encoded audio data
     * @param language Language code (e.g., "ja-JP")
     * @param audioFormat Audio format (e.g., "wav", "mp3", "ogg", "webm"). If null or empty, defaults to LINEAR16.
     */
    public Map<String, Object> speechToText(String audioData, String language, String audioFormat) {
        if (!googleCloudEnabled || speechClient == null) {
            throw new AIServiceException("Speech-to-Text", 
                "Google Cloud Speech-to-Text API is not enabled or not configured. Please enable it in application properties.",
                "SPEECH_TO_TEXT_SERVICE_DISABLED");
        }
        
        // Input validation
        if (!StringUtils.hasText(audioData)) {
            throw new AIServiceException("Speech-to-Text", "Audio data cannot be empty", "INVALID_INPUT");
        }
        
        try {
            logger.debug("Converting speech to text: language={}, audioFormat={}, audioDataLength={}", 
                language, audioFormat, audioData.length());
            
            // Decode base64 audio data
            byte[] audioBytes;
            try {
                audioBytes = Base64.getDecoder().decode(audioData);
            } catch (IllegalArgumentException e) {
                throw new AIServiceException("Speech-to-Text", "Invalid base64 audio data", "INVALID_INPUT");
            }
            ByteString audioBytesString = ByteString.copyFrom(audioBytes);
            
            // Default to Japanese for Vietnamese users learning Japanese
            String langCode = language != null && !language.isEmpty() ? language : "ja-JP";
            
            // Auto-detect audio format from magic bytes if not provided
            String detectedFormat = audioFormat;
            if (detectedFormat == null || detectedFormat.trim().isEmpty()) {
                detectedFormat = detectAudioFormat(audioBytes);
                logger.debug("Auto-detected audio format: {}", detectedFormat);
            }
            
            // Configure recognition based on audio format
            RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                .setLanguageCode(langCode)
                .setEnableAutomaticPunctuation(true);
            
            // Handle different audio formats
            String format = (detectedFormat != null) ? detectedFormat.toLowerCase().trim() : "";
            
            if (format.equals("webm") || format.equals("ogg") || format.contains("opus")) {
                // WEBM OPUS: Let Google Cloud auto-detect encoding and sample rate from header
                // Don't set encoding or sample_rate_hertz for container formats
                logger.debug("Using auto-detection for WEBM/OGG/OPUS format");
            } else if (format.equals("mp3")) {
                // MP3: Use MP3 encoding, let Google Cloud detect sample rate
                configBuilder.setEncoding(RecognitionConfig.AudioEncoding.MP3);
                logger.debug("Using MP3 encoding with auto-detected sample rate");
            } else if (format.equals("flac")) {
                // FLAC: Use FLAC encoding, let Google Cloud detect sample rate
                configBuilder.setEncoding(RecognitionConfig.AudioEncoding.FLAC);
                logger.debug("Using FLAC encoding with auto-detected sample rate");
            } else {
                // Default: LINEAR16 (WAV) with configured sample rate
                configBuilder.setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(speechToTextSampleRate);
                logger.debug("Using LINEAR16 encoding with sample rate: {}", speechToTextSampleRate);
            }
            
            RecognitionConfig config = configBuilder.build();
            
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytesString)
                .build();
            
            RecognizeRequest request = RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build();
            
            RecognizeResponse response = speechClient.recognize(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("language", langCode);
            
            if (response.getResultsCount() > 0) {
                SpeechRecognitionResult firstResult = response.getResults(0);
                if (firstResult.getAlternativesCount() > 0) {
                    SpeechRecognitionAlternative alternative = firstResult.getAlternatives(0);
                    result.put("transcript", alternative.getTranscript());
                    result.put("confidence", alternative.getConfidence());
                }
            } else {
                result.put("transcript", "");
                result.put("confidence", 0.0);
                result.put("error", "No speech detected in audio");
            }
            
            // Format response for Vietnamese users
            if (responseFormatter != null) {
                result = responseFormatter.formatSpeechToTextResponse(result);
            }
            
            logger.debug("Speech-to-text successful: transcript={}, confidence={}", 
                result.get("transcript"), result.get("confidence"));
            return result;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Speech-to-text API error", e);
            throw new AIServiceException("Speech-to-Text", "Speech-to-text conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detect audio format from magic bytes
     * WEBM: starts with 0x1A 0x45 0xDF 0xA3
     * OGG: starts with "OggS"
     * MP3: starts with 0xFF 0xFB or 0xFF 0xF3 or "ID3"
     * FLAC: starts with "fLaC"
     * WAV: starts with "RIFF" and contains "WAVE"
     */
    private String detectAudioFormat(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length < 4) {
            // Audio data too small or null - default to webm (browser recording)
            logger.warn("Audio data too small for format detection (length: {}), defaulting to webm", 
                audioBytes != null ? audioBytes.length : 0);
            return "webm";
        }
        
        // Check WEBM (EBML header: 0x1A 0x45 0xDF 0xA3)
        if (audioBytes.length >= 4 && 
            audioBytes[0] == 0x1A && audioBytes[1] == 0x45 && 
            audioBytes[2] == (byte)0xDF && audioBytes[3] == (byte)0xA3) {
            return "webm";
        }
        
        // Check OGG (starts with "OggS")
        if (audioBytes.length >= 4 && 
            audioBytes[0] == 'O' && audioBytes[1] == 'g' && 
            audioBytes[2] == 'g' && audioBytes[3] == 'S') {
            return "ogg";
        }
        
        // Check FLAC (starts with "fLaC")
        if (audioBytes.length >= 4 && 
            audioBytes[0] == 'f' && audioBytes[1] == 'L' && 
            audioBytes[2] == 'a' && audioBytes[3] == 'C') {
            return "flac";
        }
        
        // Check MP3 (starts with 0xFF 0xFB or 0xFF 0xF3 or "ID3")
        if (audioBytes.length >= 2 && audioBytes[0] == (byte)0xFF && 
            (audioBytes[1] == (byte)0xFB || audioBytes[1] == (byte)0xF3)) {
            return "mp3";
        }
        if (audioBytes.length >= 3 && 
            audioBytes[0] == 'I' && audioBytes[1] == 'D' && audioBytes[2] == '3') {
            return "mp3";
        }
        
        // Check WAV (starts with "RIFF" and contains "WAVE")
        if (audioBytes.length >= 12 && 
            audioBytes[0] == 'R' && audioBytes[1] == 'I' && 
            audioBytes[2] == 'F' && audioBytes[3] == 'F' &&
            audioBytes[8] == 'W' && audioBytes[9] == 'A' && 
            audioBytes[10] == 'V' && audioBytes[11] == 'E') {
            return "wav";
        }
        
        // Default: assume webm (most common for browser recording)
        logger.warn("Could not detect audio format from magic bytes, defaulting to webm");
        return "webm";
    }
    
    /**
     * Convert text to speech using Google Cloud Text-to-Speech API
     */
    public Map<String, Object> textToSpeech(String text, String voice, String speed) {
        if (!googleCloudEnabled || textToSpeechClient == null) {
            throw new AIServiceException("Text-to-Speech", 
                "Google Cloud Text-to-Speech API is not enabled or not configured. Please enable it in application properties.",
                "TEXT_TO_SPEECH_SERVICE_DISABLED");
        }
        
        // Input validation
        if (!StringUtils.hasText(text)) {
            throw new AIServiceException("Text-to-Speech", "Text cannot be empty", "INVALID_INPUT");
        }
        
        if (text.length() > textToSpeechMaxLength) {
            throw new AIServiceException("Text-to-Speech", "Text exceeds maximum length of " + textToSpeechMaxLength + " characters", "INVALID_INPUT");
        }
        
        try {
            logger.debug("Converting text to speech: textLength={}, voice={}, speed={}", text.length(), voice, speed);
            // Default voice for Japanese (for Vietnamese users learning Japanese)
            String voiceName = voice != null && !voice.isEmpty() ? voice : "ja-JP-Standard-A";
            
            // Parse speed (SSML speaking rate: 0.25 to 4.0)
            double speakingRate = textToSpeechSpeedNormal;
            if ("slow".equalsIgnoreCase(speed)) {
                speakingRate = textToSpeechSpeedSlow;
            } else if ("fast".equalsIgnoreCase(speed)) {
                speakingRate = textToSpeechSpeedFast;
            }
            
            // Build the voice request
            SynthesisInput input = SynthesisInput.newBuilder()
                .setText(text)
                .build();
            
            VoiceSelectionParams voiceParams = VoiceSelectionParams.newBuilder()
                .setLanguageCode("ja-JP")
                .setName(voiceName)
                .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                .build();
            
            AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .setSpeakingRate(speakingRate)
                .build();
            
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.newBuilder()
                .setInput(input)
                .setVoice(voiceParams)
                .setAudioConfig(audioConfig)
                .build();
            
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(request);
            
            // Encode audio to base64
            byte[] audioBytes = response.getAudioContent().toByteArray();
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            result.put("voice", voiceName);
            result.put("speed", speed != null ? speed : "normal");
            result.put("audioFormat", "mp3");
            result.put("audioData", audioBase64);
            result.put("audioSize", audioBytes.length);
            
            // Format response for Vietnamese users
            if (responseFormatter != null) {
                result = responseFormatter.formatTextToSpeechResponse(result);
            }
            
            logger.debug("Text-to-speech successful: audioSize={}", result.get("audioSize"));
            return result;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Text-to-speech API error", e);
            throw new AIServiceException("Text-to-Speech", "Text-to-speech conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if Google Cloud AI services are available
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("googleCloudEnabled", googleCloudEnabled);
        healthStatus.put("projectId", projectId);
        
        Map<String, String> services = new HashMap<>();
        services.put("translation", translateClient != null ? "enabled" : "disabled");
        services.put("sentiment", languageServiceClient != null ? "enabled" : "disabled");
        services.put("contentGeneration", "partial (using sentiment analysis)");
        services.put("speechToText", speechClient != null ? "enabled" : "disabled");
        services.put("textToSpeech", textToSpeechClient != null ? "enabled" : "disabled");
        
        healthStatus.put("services", services);
        
        if (googleCloudEnabled && 
            translateClient != null && 
            languageServiceClient != null && 
            speechClient != null && 
            textToSpeechClient != null) {
            healthStatus.put("status", "READY");
            healthStatus.put("message", "All Google Cloud AI services are ready");
        } else if (googleCloudEnabled) {
            healthStatus.put("status", "PARTIAL");
            healthStatus.put("message", "Some Google Cloud AI services are not available. Please check credentials and API enablement.");
        } else {
            healthStatus.put("status", "DISABLED");
            healthStatus.put("message", "Google Cloud AI is disabled. Set google.cloud.enabled=true to enable.");
        }
        
        healthStatus.put("timestamp", java.time.LocalDateTime.now());
        return healthStatus;
    }
    
    /**
     * Helper method to convert sentiment score to label
     */
    private String getSentimentLabel(float score) {
        if (score >= sentimentThresholdPositive) {
            return "POSITIVE";
        } else if (score <= sentimentThresholdNegative) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }
}