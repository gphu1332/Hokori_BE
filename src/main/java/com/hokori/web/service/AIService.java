package com.hokori.web.service;

import com.google.cloud.language.v1.*;
import com.google.cloud.speech.v1.*;
import com.google.cloud.texttospeech.v1.*;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Google Cloud AI integration
 */
@Service
public class AIService {
    
    @Value("${google.cloud.project-id:hokori-web}")
    private String projectId;
    
    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;
    
    @Autowired(required = false)
    private Translate translateClient;
    
    @Autowired(required = false)
    private LanguageServiceClient languageServiceClient;
    
    @Autowired(required = false)
    private SpeechClient speechClient;
    
    @Autowired(required = false)
    private TextToSpeechClient textToSpeechClient;
    
    /**
     * Translate text using Google Cloud Translation API
     */
    public Map<String, Object> translateText(String text, String sourceLanguage, String targetLanguage) {
        if (!googleCloudEnabled || translateClient == null) {
            throw new RuntimeException("Google Cloud Translation API is not enabled or not configured. Please enable it in application properties.");
        }
        
        try {
            // Default to English if target language not specified
            String targetLang = targetLanguage != null && !targetLanguage.isEmpty() ? targetLanguage : "en";
            
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
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyze text sentiment using Google Cloud Natural Language API
     */
    public Map<String, Object> analyzeSentiment(String text) {
        if (!googleCloudEnabled || languageServiceClient == null) {
            throw new RuntimeException("Google Cloud Natural Language API is not enabled or not configured. Please enable it in application properties.");
        }
        
        try {
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
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Sentiment analysis failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate content using AI (using Natural Language API for now, can be extended with Vertex AI)
     */
    public Map<String, Object> generateContent(String prompt, String contentType) {
        if (!googleCloudEnabled) {
            throw new RuntimeException("Google Cloud AI is not enabled. Please enable it in application properties.");
        }
        
        // For now, we'll use sentiment analysis as a placeholder
        // In the future, this can be extended with Vertex AI for content generation
        try {
            Map<String, Object> sentimentResult = analyzeSentiment(prompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("prompt", prompt);
            result.put("contentType", contentType);
            result.put("generatedContent", "Content generation feature is under development. Currently analyzing sentiment: " + sentimentResult.get("sentiment"));
            result.put("note", "This endpoint will use Vertex AI for content generation in future updates.");
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Content generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert speech to text using Google Cloud Speech-to-Text API
     */
    public Map<String, Object> speechToText(String audioData, String language) {
        if (!googleCloudEnabled || speechClient == null) {
            throw new RuntimeException("Google Cloud Speech-to-Text API is not enabled or not configured. Please enable it in application properties.");
        }
        
        try {
            // Decode base64 audio data
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            ByteString audioBytesString = ByteString.copyFrom(audioBytes);
            
            // Default to Japanese if language not specified
            String langCode = language != null && !language.isEmpty() ? language : "ja-JP";
            
            // Configure recognition
            RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode(langCode)
                .setEnableAutomaticPunctuation(true)
                .build();
            
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
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Speech-to-text conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert text to speech using Google Cloud Text-to-Speech API
     */
    public Map<String, Object> textToSpeech(String text, String voice, String speed) {
        if (!googleCloudEnabled || textToSpeechClient == null) {
            throw new RuntimeException("Google Cloud Text-to-Speech API is not enabled or not configured. Please enable it in application properties.");
        }
        
        try {
            // Default voice for Japanese
            String voiceName = voice != null && !voice.isEmpty() ? voice : "ja-JP-Standard-A";
            
            // Parse speed (SSML speaking rate: 0.25 to 4.0)
            double speakingRate = 1.0; // normal speed
            if ("slow".equalsIgnoreCase(speed)) {
                speakingRate = 0.75;
            } else if ("fast".equalsIgnoreCase(speed)) {
                speakingRate = 1.5;
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
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Text-to-speech conversion failed: " + e.getMessage(), e);
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
        if (score >= 0.25) {
            return "POSITIVE";
        } else if (score <= -0.25) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }
}