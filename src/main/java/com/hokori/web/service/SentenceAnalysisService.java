package com.hokori.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokori.web.dto.GrammarItem;
import com.hokori.web.dto.SentenceAnalysisResponse;
import com.hokori.web.dto.VocabularyItem;
import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for AI Sentence Analysis feature
 * Analyzes Japanese sentences for Vietnamese users learning Japanese
 * Provides vocabulary and grammar analysis with Vietnamese explanations
 * Target audience: Vietnamese users only (Vietnamese → Japanese learning)
 */
@Service
public class SentenceAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(SentenceAnalysisService.class);

    @Value("${google.cloud.project-id:hokori-web}")
    private String projectId;

    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;

    @Value("${google.cloud.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${google.cloud.gemini.model:gemini-pro}")
    private String geminiModel;

    @Value("${ai.sentence-analysis.max-length:100}")
    private int maxSentenceLength;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyze Japanese sentence for vocabulary and grammar
     *
     * @param sentence Japanese sentence to analyze (15-30 characters recommended)
     * @param level User's JLPT level (N5-N1)
     * @return SentenceAnalysisResponse with vocabulary and grammar analysis
     */
    public SentenceAnalysisResponse analyzeSentence(String sentence, String level) {
        if (!googleCloudEnabled) {
            throw new AIServiceException("Sentence Analysis",
                "Google Cloud AI is not enabled. Please enable it in application properties.",
                "SENTENCE_ANALYSIS_SERVICE_DISABLED");
        }

        // Validate API key
        if (!StringUtils.hasText(geminiApiKey)) {
            throw new AIServiceException("Sentence Analysis",
                "Gemini API key is not configured. Please set GOOGLE_CLOUD_GEMINI_API_KEY environment variable.",
                "GEMINI_API_KEY_NOT_CONFIGURED");
        }

        // Input validation
        if (!StringUtils.hasText(sentence)) {
            throw new AIServiceException("Sentence Analysis", "Sentence cannot be empty", "INVALID_INPUT");
        }

        if (sentence.length() > maxSentenceLength) {
            throw new AIServiceException("Sentence Analysis",
                "Sentence exceeds maximum length of " + maxSentenceLength + " characters", "INVALID_INPUT");
        }

        String normalizedLevel = normalizeLevel(level);
        logger.info("Analyzing sentence: sentenceLength={}, level={}", sentence.length(), normalizedLevel);

        try {
            // Step 1: Analyze vocabulary
            List<VocabularyItem> vocabulary = analyzeVocabulary(sentence, normalizedLevel);

            // Step 2: Analyze grammar
            List<GrammarItem> grammar = analyzeGrammar(sentence, normalizedLevel);

            // Step 3: Build response
            SentenceAnalysisResponse response = new SentenceAnalysisResponse();
            response.setSentence(sentence);
            response.setLevel(normalizedLevel);
            response.setVocabulary(vocabulary);
            response.setGrammar(grammar);

            logger.debug("Sentence analysis completed: vocabularyCount={}, grammarCount={}",
                vocabulary.size(), grammar.size());

            return response;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Sentence analysis failed", e);
            throw new AIServiceException("Sentence Analysis",
                "Sentence analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze vocabulary in the sentence using AI
     */
    private List<VocabularyItem> analyzeVocabulary(String sentence, String level) {
        String prompt = buildVocabularyPrompt(sentence, level);
        String jsonResponse = callGeminiAPI(prompt);

        try {
            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            
            // Extract vocabulary array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vocabularyList = (List<Map<String, Object>>) responseMap.get("vocabulary");
            
            if (vocabularyList == null) {
                logger.warn("No vocabulary found in AI response");
                return new ArrayList<>();
            }

            // Convert to VocabularyItem list
            List<VocabularyItem> vocabulary = new ArrayList<>();
            for (Map<String, Object> item : vocabularyList) {
                VocabularyItem vocabItem = mapToVocabularyItem(item, level);
                if (vocabItem != null) {
                    vocabulary.add(vocabItem);
                }
            }

            return vocabulary;
        } catch (Exception e) {
            logger.error("Failed to parse vocabulary response", e);
            throw new AIServiceException("Sentence Analysis",
                "Failed to parse vocabulary analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze grammar patterns in the sentence using AI
     */
    private List<GrammarItem> analyzeGrammar(String sentence, String level) {
        String prompt = buildGrammarPrompt(sentence, level);
        String jsonResponse = callGeminiAPI(prompt);

        try {
            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            
            // Extract grammar array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grammarList = (List<Map<String, Object>>) responseMap.get("grammar");
            
            if (grammarList == null) {
                logger.warn("No grammar found in AI response");
                return new ArrayList<>();
            }

            // Convert to GrammarItem list
            List<GrammarItem> grammar = new ArrayList<>();
            for (Map<String, Object> item : grammarList) {
                GrammarItem grammarItem = mapToGrammarItem(item);
                if (grammarItem != null) {
                    grammar.add(grammarItem);
                }
            }

            return grammar;
        } catch (Exception e) {
            logger.error("Failed to parse grammar response", e);
            throw new AIServiceException("Sentence Analysis",
                "Failed to parse grammar analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt for vocabulary analysis
     */
    private String buildVocabularyPrompt(String sentence, String level) {
        return String.format(
            "You are analyzing Japanese vocabulary for Vietnamese users learning Japanese.\n\n" +
            "Analyze the vocabulary in this Japanese sentence: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Extract all words and provide detailed information in JSON format:\n" +
            "{\n" +
            "  \"vocabulary\": [\n" +
            "    {\n" +
            "      \"word\": \"Japanese word\",\n" +
            "      \"reading\": \"hiragana reading\",\n" +
            "      \"meaning_vi\": \"Vietnamese meaning (MUST be in Vietnamese, not English)\",\n" +
            "      \"jlpt_level\": \"N5|N4|N3|N2|N1\",\n" +
            "      \"kanji_details\": {\n" +
            "        \"radical\": \"radical (if kanji)\",\n" +
            "        \"stroke_count\": number,\n" +
            "        \"onyomi\": \"onyomi reading\",\n" +
            "        \"kunyomi\": \"kunyomi reading\",\n" +
            "        \"related_words\": [\"word1\", \"word2\"]\n" +
            "      },\n" +
            "      \"importance\": \"high|medium|low\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL meanings MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- This is for Vietnamese users learning Japanese\n" +
            "- Mark words as \"high\" importance if they match user's level (%s) or are essential\n" +
            "- Mark words as \"medium\" if they are slightly above user's level\n" +
            "- Mark words as \"low\" if they are well below user's level\n" +
            "- Only include kanji_details if the word contains kanji\n" +
            "- Return ONLY valid JSON, no additional text",
            sentence, level, level);
    }

    /**
     * Build prompt for grammar analysis
     */
    private String buildGrammarPrompt(String sentence, String level) {
        return String.format(
            "You are analyzing Japanese grammar for Vietnamese users learning Japanese.\n\n" +
            "Analyze the grammar patterns in this Japanese sentence: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Identify all grammar patterns and provide detailed information in JSON format:\n" +
            "{\n" +
            "  \"grammar\": [\n" +
            "    {\n" +
            "      \"pattern\": \"grammar pattern name\",\n" +
            "      \"jlpt_level\": \"N5|N4|N3|N2|N1\",\n" +
            "      \"explanation_vi\": \"Vietnamese explanation (MUST be in Vietnamese, not English)\",\n" +
            "      \"example\": \"example sentence\",\n" +
            "      \"notes\": \"common mistakes and notes (in Vietnamese)\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL explanations and notes MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- This is for Vietnamese users learning Japanese\n" +
            "- Focus on grammar patterns appropriate for level %s\n" +
            "- Provide clear explanations in Vietnamese\n" +
            "- Include practical examples\n" +
            "- Return ONLY valid JSON, no additional text",
            sentence, level, level);
    }

    /**
     * Call Gemini API to get AI analysis
     */
    private String callGeminiAPI(String prompt) {
        try {
            String apiUrl = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.debug("Calling Gemini API: model={}", geminiModel);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    
                    if (contentMap != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> partsList = (List<Map<String, Object>>) contentMap.get("parts");
                        
                        if (partsList != null && !partsList.isEmpty()) {
                            String text = (String) partsList.get(0).get("text");
                            // Extract JSON from response (remove markdown code blocks if present)
                            return extractJsonFromText(text);
                        }
                    }
                }
            }

            // Check for error in response
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) body.get("error");
                    String errorMessage = error != null ? (String) error.get("message") : "Unknown error";
                    throw new AIServiceException("Sentence Analysis",
                        "Gemini API error: " + errorMessage, "GEMINI_API_ERROR");
                }
            }

            throw new AIServiceException("Sentence Analysis",
                "Invalid response from Gemini API", "AI_API_ERROR");
        } catch (AIServiceException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("Gemini API HTTP error: {}", e.getResponseBodyAsString());
            throw new AIServiceException("Sentence Analysis",
                "Gemini API HTTP error: " + e.getMessage(), "GEMINI_API_HTTP_ERROR");
        } catch (Exception e) {
            logger.error("Gemini API call failed", e);
            throw new AIServiceException("Sentence Analysis",
                "Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Extract JSON from AI response text (remove markdown code blocks)
     */
    private String extractJsonFromText(String text) {
        if (text == null) {
            return "{}";
        }

        // Remove markdown code blocks if present
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }

        return text.trim();
    }

    /**
     * Convert map to VocabularyItem
     */
    private VocabularyItem mapToVocabularyItem(Map<String, Object> map, String userLevel) {
        try {
            VocabularyItem item = new VocabularyItem();
            item.setWord((String) map.get("word"));
            item.setReading((String) map.get("reading"));
            item.setMeaningVi((String) map.get("meaning_vi"));
            item.setJlptLevel((String) map.get("jlpt_level"));
            item.setImportance(determineImportance((String) map.get("importance"), 
                (String) map.get("jlpt_level"), userLevel));

            // Handle kanji_details
            @SuppressWarnings("unchecked")
            Map<String, Object> kanjiMap = (Map<String, Object>) map.get("kanji_details");
            if (kanjiMap != null) {
                VocabularyItem.KanjiDetails kanjiDetails = new VocabularyItem.KanjiDetails();
                kanjiDetails.setRadical((String) kanjiMap.get("radical"));
                if (kanjiMap.get("stroke_count") != null) {
                    kanjiDetails.setStrokeCount(((Number) kanjiMap.get("stroke_count")).intValue());
                }
                kanjiDetails.setOnyomi((String) kanjiMap.get("onyomi"));
                kanjiDetails.setKunyomi((String) kanjiMap.get("kunyomi"));
                @SuppressWarnings("unchecked")
                List<String> relatedWords = (List<String>) kanjiMap.get("related_words");
                kanjiDetails.setRelatedWords(relatedWords);
                item.setKanjiDetails(kanjiDetails);
            }

            return item;
        } catch (Exception e) {
            logger.warn("Failed to map vocabulary item: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert map to GrammarItem
     */
    private GrammarItem mapToGrammarItem(Map<String, Object> map) {
        try {
            GrammarItem item = new GrammarItem();
            item.setPattern((String) map.get("pattern"));
            item.setJlptLevel((String) map.get("jlpt_level"));
            item.setExplanationVi((String) map.get("explanation_vi"));
            item.setExample((String) map.get("example"));
            item.setNotes((String) map.get("notes"));
            return item;
        } catch (Exception e) {
            logger.warn("Failed to map grammar item: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Determine importance based on word level and user level
     */
    private String determineImportance(String importance, String wordLevel, String userLevel) {
        if (importance != null && !importance.isEmpty()) {
            return importance.toLowerCase();
        }

        // Auto-determine based on levels
        int userLevelNum = getLevelNumber(userLevel);
        int wordLevelNum = getLevelNumber(wordLevel);

        if (wordLevelNum <= userLevelNum) {
            return "high";
        } else if (wordLevelNum == userLevelNum + 1) {
            return "medium";
        } else {
            return "low";
        }
    }

    /**
     * Get numeric value for JLPT level
     */
    private int getLevelNumber(String level) {
        if (level == null) {
            return 5; // Default to N5
        }
        switch (level.toUpperCase()) {
            case "N1": return 1;
            case "N2": return 2;
            case "N3": return 3;
            case "N4": return 4;
            case "N5": return 5;
            default: return 5;
        }
    }

    /**
     * Normalize JLPT level
     */
    private String normalizeLevel(String level) {
        if (level == null || level.isEmpty()) {
            return "N5";
        }
        String upperLevel = level.toUpperCase();
        if (upperLevel.equals("N5") || upperLevel.equals("N4") ||
            upperLevel.equals("N3") || upperLevel.equals("N2") || upperLevel.equals("N1")) {
            return upperLevel;
        }
        return "N5"; // Default to N5
    }
}

