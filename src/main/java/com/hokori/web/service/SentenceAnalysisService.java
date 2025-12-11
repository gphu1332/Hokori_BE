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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Value("${ai.sentence-analysis.max-length:50}")
    private int maxSentenceLength;

    @Autowired(required = false)
    private GeminiService geminiService;

    @Autowired(required = false)
    private AIService aiService;

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

        // Validate GeminiService is available
        if (geminiService == null) {
            throw new AIServiceException("Sentence Analysis",
                "Gemini service is not available. Please ensure Google Cloud is properly configured.",
                "GEMINI_SERVICE_NOT_AVAILABLE");
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
            // Step 0: Detect language and translate accordingly
            String japaneseSentence = sentence;
            String originalSentence = null;
            String vietnameseTranslation = null;
            boolean isTranslated = false;
            
            logger.info("Detecting language for input: '{}'", sentence);
            String detectedLanguage = detectLanguage(sentence);
            logger.info("Detected language: {}", detectedLanguage);
            
            // Only support Vietnamese and Japanese - reject other languages
            if (!"vi".equals(detectedLanguage) && !"ja".equals(detectedLanguage)) {
                throw new AIServiceException("Sentence Analysis",
                    "Chỉ hỗ trợ tiếng Việt và tiếng Nhật. Vui lòng nhập câu tiếng Việt hoặc tiếng Nhật.",
                    "UNSUPPORTED_LANGUAGE");
            }
            
            if ("vi".equals(detectedLanguage)) {
                // Input is Vietnamese → Translate to Japanese
                logger.info("Vietnamese detected! Translating to Japanese for level: {}", normalizedLevel);
                originalSentence = sentence;
                try {
                    japaneseSentence = translateVietnameseToJapanese(sentence, normalizedLevel);
                    isTranslated = true;
                    logger.info("✅ Successfully translated Vietnamese to Japanese: '{}' -> '{}'", originalSentence, japaneseSentence);
                } catch (Exception e) {
                    logger.error("❌ Translation failed: {}", e.getMessage(), e);
                    throw new AIServiceException("Sentence Analysis",
                        "Không thể dịch câu tiếng Việt sang tiếng Nhật: " + e.getMessage(), e);
                }
            } else {
                // Input is Japanese → Translate to Vietnamese for display
                // May contain Latin characters (e.g., "JLPT", "N5", abbreviations) - that's OK
                logger.info("Japanese detected! Translating to Vietnamese for better understanding");
                try {
                    vietnameseTranslation = translateJapaneseToVietnamese(japaneseSentence);
                    logger.info("✅ Successfully translated Japanese to Vietnamese: '{}' -> '{}'", japaneseSentence, vietnameseTranslation);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to translate Japanese to Vietnamese (non-critical): {}", e.getMessage());
                    // Translation to Vietnamese is optional, continue without it
                    vietnameseTranslation = null;
                }
            }

            // Step 1: Analyze vocabulary (handles mixed Japanese + Latin characters)
            // Note: Latin characters like "JLPT", "N5" are common in Japanese learning context
            List<VocabularyItem> vocabulary = analyzeVocabulary(japaneseSentence, normalizedLevel);

            // Step 2: Analyze grammar
            List<GrammarItem> grammar = analyzeGrammar(japaneseSentence, normalizedLevel);

            // Step 3: Analyze sentence breakdown
            SentenceAnalysisResponse.SentenceBreakdown breakdown = analyzeSentenceBreakdown(japaneseSentence, normalizedLevel);

            // Step 4: Get related sentences
            List<String> relatedSentences = getRelatedSentences(japaneseSentence, normalizedLevel);

            // Step 5: Build response
            SentenceAnalysisResponse response = new SentenceAnalysisResponse();
            response.setSentence(japaneseSentence);
            response.setOriginalSentence(isTranslated ? originalSentence : null);
            response.setIsTranslated(isTranslated);
            response.setVietnameseTranslation(vietnameseTranslation);
            response.setLevel(normalizedLevel);
            response.setVocabulary(vocabulary);
            response.setGrammar(grammar);
            response.setSentenceBreakdown(breakdown);
            response.setRelatedSentences(relatedSentences);

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
        String jsonResponse = geminiService.generateContent(prompt);
        
        // Extract JSON from response (remove markdown code blocks if present)
        jsonResponse = extractJsonFromText(jsonResponse);

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
        String jsonResponse = geminiService.generateContent(prompt);
        
        // Extract JSON from response (remove markdown code blocks if present)
        jsonResponse = extractJsonFromText(jsonResponse);

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
            "Focus on NOTABLE and IMPORTANT vocabulary that:\n" +
            "- Matches user's level (%s) or is essential for understanding\n" +
            "- Contains interesting kanji or grammar patterns\n" +
            "- Is worth learning at this level\n\n" +
            "Extract notable words and provide detailed information in JSON format:\n" +
            "{\n" +
            "  \"vocabulary\": [\n" +
            "    {\n" +
            "      \"word\": \"Japanese word\",\n" +
            "      \"reading\": \"hiragana reading\",\n" +
            "      \"meaning_vi\": \"Vietnamese meaning (MUST be in Vietnamese, not English)\",\n" +
            "      \"jlpt_level\": \"N5|N4|N3|N2|N1\",\n" +
            "      \"kanji_details\": {\n" +
            "        \"radical\": \"radical (if kanji, break down by components appropriate for level %s)\",\n" +
            "        \"stroke_count\": number,\n" +
            "        \"onyomi\": \"onyomi reading\",\n" +
            "        \"kunyomi\": \"kunyomi reading\",\n" +
            "        \"related_words\": [\"word1\", \"word2\"]\n" +
            "      },\n" +
            "      \"importance\": \"high|medium|low\",\n" +
            "      \"examples\": [\"example sentence 1\", \"example sentence 2\"],\n" +
            "      \"kanji_variants\": [\"kanji form\", \"hiragana form\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL meanings MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- This is for Vietnamese users learning Japanese\n" +
            "- Focus on words that are NOTABLE and WORTH LEARNING at level %s\n" +
            "- Mark words as \"high\" importance if they match user's level (%s) or are essential\n" +
            "- Mark words as \"medium\" if they are slightly above user's level (good to learn)\n" +
            "- Skip very basic/common words that are well below user's level (unless they have interesting kanji)\n" +
            "- If word is hiragana (e.g., わたし), provide kanji_variants with kanji form (e.g., [\"私\", \"わたし\"])\n" +
            "- If word is kanji, provide kanji_variants with hiragana reading and alternative kanji forms\n" +
            "- Break down kanji radicals/components appropriate for user's level (%s)\n" +
            "- Provide 2-3 example sentences using this vocabulary\n" +
            "- Only include kanji_details if the word contains kanji\n" +
            "- Return ONLY valid JSON, no additional text",
            sentence, level, level, level, level, level, level);
    }

    /**
     * Build prompt for grammar analysis
     */
    private String buildGrammarPrompt(String sentence, String level) {
        return String.format(
            "You are analyzing Japanese grammar for Vietnamese users learning Japanese.\n\n" +
            "Analyze the grammar patterns in this Japanese sentence: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Focus on NOTABLE and IMPORTANT grammar patterns that:\n" +
            "- Match user's level (%s) or are essential for understanding\n" +
            "- Are worth learning and practicing at this level\n" +
            "- Have clear patterns that can be explained\n\n" +
            "Identify notable grammar patterns and provide detailed information in JSON format:\n" +
            "{\n" +
            "  \"grammar\": [\n" +
            "    {\n" +
            "      \"pattern\": \"grammar pattern name\",\n" +
            "      \"jlpt_level\": \"N5|N4|N3|N2|N1\",\n" +
            "      \"explanation_vi\": \"Vietnamese explanation (MUST be in Vietnamese, not English)\",\n" +
            "      \"example\": \"example sentence\",\n" +
            "      \"notes\": \"common mistakes and notes (in Vietnamese)\",\n" +
            "      \"examples\": [\"example sentence 1\", \"example sentence 2\"],\n" +
            "      \"confusing_patterns\": [\n" +
            "        {\n" +
            "          \"pattern\": \"pattern name that might be confused\",\n" +
            "          \"difference\": \"explanation of difference in Vietnamese\",\n" +
            "          \"example\": \"example sentence showing the difference\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL explanations and notes MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- This is for Vietnamese users learning Japanese\n" +
            "- Focus on grammar patterns appropriate for level %s\n" +
            "- Provide clear explanations in Vietnamese\n" +
            "- Include 2-3 example sentences using this grammar pattern\n" +
            "- Identify confusing patterns at the same JLPT level (%s) that might be mistaken\n" +
            "- For confusing_patterns, focus on patterns that Vietnamese learners commonly confuse\n" +
            "- Explain the difference clearly in Vietnamese\n" +
            "- Return ONLY valid JSON, no additional text",
            sentence, level, level, level, level);
    }

    /**
     * Analyze sentence breakdown (structure analysis)
     */
    private SentenceAnalysisResponse.SentenceBreakdown analyzeSentenceBreakdown(String sentence, String level) {
        String prompt = buildSentenceBreakdownPrompt(sentence, level);
        String jsonResponse = geminiService.generateContent(prompt);
        
        // Extract JSON from response (remove markdown code blocks if present)
        jsonResponse = extractJsonFromText(jsonResponse);

        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> breakdownMap = (Map<String, Object>) responseMap.get("breakdown");
            
            if (breakdownMap == null) {
                logger.warn("No breakdown found in AI response");
                return null;
            }

            SentenceAnalysisResponse.SentenceBreakdown breakdown = new SentenceAnalysisResponse.SentenceBreakdown();
            breakdown.setSubject((String) breakdownMap.get("subject"));
            breakdown.setPredicate((String) breakdownMap.get("predicate"));
            breakdown.setObject((String) breakdownMap.get("object"));
            breakdown.setExplanationVi((String) breakdownMap.get("explanation_vi"));
            
            @SuppressWarnings("unchecked")
            List<String> particles = (List<String>) breakdownMap.get("particles");
            breakdown.setParticles(particles);

            return breakdown;
        } catch (Exception e) {
            logger.error("Failed to parse sentence breakdown", e);
            return null; // Return null if parsing fails, don't break the whole response
        }
    }

    /**
     * Get related example sentences
     */
    private List<String> getRelatedSentences(String sentence, String level) {
        String prompt = buildRelatedSentencesPrompt(sentence, level);
        String jsonResponse = geminiService.generateContent(prompt);
        
        // Extract JSON from response (remove markdown code blocks if present)
        jsonResponse = extractJsonFromText(jsonResponse);

        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<String> sentences = (List<String>) responseMap.get("related_sentences");
            
            return sentences != null ? sentences : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Failed to parse related sentences", e);
            return new ArrayList<>(); // Return empty list if parsing fails
        }
    }

    /**
     * Get example sentences for sentence analysis practice
     * These are sentences suitable for vocabulary and grammar analysis (not conversation practice)
     * 
     * @param level JLPT level (N5-N1)
     * @return List of example sentences with translations
     */
    public List<Map<String, Object>> getExampleSentences(String level) {
        if (!googleCloudEnabled) {
            throw new AIServiceException("Sentence Analysis",
                "Google Cloud AI is not enabled. Please enable it in application properties.",
                "SENTENCE_ANALYSIS_SERVICE_DISABLED");
        }

        if (geminiService == null) {
            throw new AIServiceException("Sentence Analysis",
                "Gemini service is not available. Please ensure Google Cloud is properly configured.",
                "GEMINI_SERVICE_NOT_AVAILABLE");
        }

        String normalizedLevel = normalizeLevel(level);
        logger.info("Getting example sentences for sentence analysis: level={}", normalizedLevel);

        try {
            String prompt = buildExampleSentencesPrompt(normalizedLevel);
            String jsonResponse = geminiService.generateContent(prompt);
            
            // Extract JSON from response
            jsonResponse = extractJsonFromText(jsonResponse);

            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sentences = (List<Map<String, Object>>) responseMap.get("sentences");
            
            return sentences != null ? sentences : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Failed to get example sentences", e);
            throw new AIServiceException("Sentence Analysis",
                "Failed to get example sentences: " + e.getMessage(), e);
        }
    }

    /**
     * Get a random example sentence for sentence analysis practice
     */
    public Map<String, Object> getRandomExampleSentence(String level) {
        List<Map<String, Object>> sentences = getExampleSentences(level);
        if (sentences.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return sentences.get(random.nextInt(sentences.size()));
    }

    /**
     * Build prompt for generating example sentences for sentence analysis
     */
    private String buildExampleSentencesPrompt(String level) {
        return String.format(
            "You are generating example Japanese sentences for Vietnamese users learning Japanese.\n\n" +
            "Generate 10-15 example sentences suitable for VOCABULARY AND GRAMMAR ANALYSIS (not conversation practice).\n\n" +
            "User's JLPT level: %s\n\n" +
            "Requirements:\n" +
            "- Sentences should contain interesting vocabulary and grammar patterns appropriate for level %s\n" +
            "- Focus on sentences that demonstrate clear grammar structures and useful vocabulary\n" +
            "- Sentences should be 15-40 characters long\n" +
            "- Include a mix of common patterns and vocabulary for level %s\n" +
            "- NOT conversational phrases (like greetings or small talk)\n" +
            "- Examples: sentences about daily activities, descriptions, explanations, etc.\n\n" +
            "Provide in JSON format:\n" +
            "{\n" +
            "  \"sentences\": [\n" +
            "    {\n" +
            "      \"sentence\": \"Japanese sentence\",\n" +
            "      \"translation\": \"Vietnamese translation\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL translations MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- Return ONLY valid JSON, no additional text",
            level, level, level);
    }

    /**
     * Build prompt for sentence breakdown
     */
    private String buildSentenceBreakdownPrompt(String sentence, String level) {
        return String.format(
            "You are analyzing Japanese sentence structure for Vietnamese users learning Japanese.\n\n" +
            "Analyze the structure of this Japanese sentence: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Provide detailed breakdown in JSON format:\n" +
            "{\n" +
            "  \"breakdown\": {\n" +
            "    \"subject\": \"subject (if any)\",\n" +
            "    \"predicate\": \"predicate/verb\",\n" +
            "    \"object\": \"object (if any)\",\n" +
            "    \"particles\": [\"は\", \"を\", \"に\"],\n" +
            "    \"explanation_vi\": \"Detailed explanation in Vietnamese of sentence structure\"\n" +
            "  }\n" +
            "}\n\n" +
            "Important:\n" +
            "- ALL explanations MUST be in Vietnamese (Tiếng Việt), NOT English\n" +
            "- Explain the role of each particle clearly\n" +
            "- Explain sentence structure appropriate for level %s\n" +
            "- Return ONLY valid JSON, no additional text",
            sentence, level, level);
    }

    /**
     * Build prompt for related sentences
     */
    private String buildRelatedSentencesPrompt(String sentence, String level) {
        return String.format(
            "You are generating related example sentences for Vietnamese users learning Japanese.\n\n" +
            "Original sentence: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Generate 3-5 related example sentences that:\n" +
            "- Use similar vocabulary and grammar patterns\n" +
            "- Are appropriate for JLPT level %s\n" +
            "- Help users understand the original sentence better\n\n" +
            "Provide in JSON format:\n" +
            "{\n" +
            "  \"related_sentences\": [\"sentence 1\", \"sentence 2\", \"sentence 3\"]\n" +
            "}\n\n" +
            "Return ONLY valid JSON, no additional text",
            sentence, level, level);
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

            // Handle examples
            @SuppressWarnings("unchecked")
            List<String> examples = (List<String>) map.get("examples");
            item.setExamples(examples);

            // Handle kanji_variants
            @SuppressWarnings("unchecked")
            List<String> kanjiVariants = (List<String>) map.get("kanji_variants");
            item.setKanjiVariants(kanjiVariants);

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

            // Handle examples
            @SuppressWarnings("unchecked")
            List<String> examples = (List<String>) map.get("examples");
            item.setExamples(examples);

            // Handle confusing_patterns
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> confusingPatternsList = (List<Map<String, Object>>) map.get("confusing_patterns");
            if (confusingPatternsList != null) {
                List<GrammarItem.ConfusingPattern> confusingPatterns = new ArrayList<>();
                for (Map<String, Object> cpMap : confusingPatternsList) {
                    GrammarItem.ConfusingPattern cp = new GrammarItem.ConfusingPattern();
                    cp.setPattern((String) cpMap.get("pattern"));
                    cp.setDifference((String) cpMap.get("difference"));
                    cp.setExample((String) cpMap.get("example"));
                    confusingPatterns.add(cp);
                }
                item.setConfusingPatterns(confusingPatterns);
            }

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

    /**
     * Detect language of input text
     * Returns language code: "ja" for Japanese, "vi" for Vietnamese, "unknown" if cannot detect
     */
    private String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }

        String normalizedText = text.toLowerCase().trim();

        // Priority 1: Check for Japanese characters FIRST (most reliable indicator)
        // This must come BEFORE Google Translate API to avoid false positives
        // If text contains Japanese characters, it's Japanese (even if mixed with Latin characters)
        // Common cases: "ありがとうございますHI" (typo/spam) or "私は JLPT の試験..." (common abbreviations)
        boolean hasJapanese = text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*");
        if (hasJapanese) {
            logger.debug("Detected Japanese by character check (may contain Latin characters like JLPT, N5, etc.)");
            return "ja";
        }

        // Priority 2: If AIService is available and no Japanese characters, use Google Translate API to detect language
        // This helps distinguish Vietnamese from English and other languages
        if (aiService != null && googleCloudEnabled) {
            try {
                Map<String, Object> translationResult = aiService.translateText(text, null, "ja");
                String detectedLang = (String) translationResult.get("detectedSourceLanguage");
                if (detectedLang != null && !detectedLang.isEmpty()) {
                    logger.info("Detected language using Translation API: {}", detectedLang);
                    // Only accept Vietnamese or Japanese - reject other languages
                    if ("vi".equals(detectedLang) || "ja".equals(detectedLang)) {
                        return detectedLang;
                    } else {
                        // Reject English and other languages
                        logger.warn("Unsupported language detected: {} (only Vietnamese and Japanese are supported)", detectedLang);
                        return "unsupported";
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to detect language using Translation API, falling back to heuristic: {}", e.getMessage());
            }
        }

        // Priority 3: Check for Vietnamese characters (with diacritics)
        boolean hasVietnameseDiacritics = text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ].*");
        if (hasVietnameseDiacritics) {
            logger.debug("Detected Vietnamese by diacritics");
            return "vi";
        }

        // Priority 4: Check for common Vietnamese words (with or without diacritics)
        // Common Vietnamese words/phrases that indicate Vietnamese language
        // Expanded list to better detect Vietnamese without diacritics
        String[] vietnameseIndicators = {
            // Pronouns
            "toi", "tôi", "tui", "minh", "mình", "ban", "bạn", "anh", "chi", "chị", "em", "co", "cô", "ong", "ông", "ba", "bà",
            "chung", "chúng", "ta", "chung ta", "chúng ta", "may", "mày", "no", "nó", "ho", "họ",
            // Verbs - common actions
            "dang", "đang", "se", "sẽ", "da", "đã", "sap", "sắp", "vua", "vừa",
            "la", "là", "lam", "làm", "hoc", "học", "doc", "đọc", "viet", "viết", "noi", "nói", "nghe", "xem",
            "di", "đi", "den", "đến", "ve", "về", "o", "ở", "co", "có", "khong", "không",
            "an", "ăn", "uong", "uống", "ngu", "ngủ", "thuc", "thức", "day", "dậy",
            // Nouns - common topics
            "tieng", "tiếng", "nhat", "nhật", "viet", "việt", "anh", "anh", "phap", "pháp", "han", "hàn",
            "nguoi", "người", "nha", "nhà", "truong", "trường", "lop", "lớp", "ban", "bạn", "thay", "thầy", "co", "cô",
            // Prepositions/Conjunctions
            "cua", "của", "voi", "với", "va", "và", "hoac", "hoặc", "neu", "nếu", "thi", "thì",
            "trong", "ngoai", "ngoài", "tren", "trên", "duoi", "dưới", "truoc", "trước", "sau",
            // Numbers
            "mot", "một", "hai", "ba", "bon", "bốn", "nam", "năm", "sau", "sáu", "bay", "bảy", "tam", "tám", "chin", "chín", "muoi", "mười",
            // Time words
            "ngay", "ngày", "thang", "tháng", "nam", "năm", "gio", "giờ", "phut", "phút", "giay", "giây",
            "hom", "hôm", "nay", "nay", "mai", "ngay mai", "ngày mai", "qua", "qua", "sang", "sáng", "trua", "trưa", "chieu", "chiều", "toi", "tối",
            // Common phrases
            "xin chao", "xin chào", "cam on", "cảm ơn", "xin loi", "xin lỗi", "khong sao", "không sao",
            "rat", "rất", "rat la", "rất là", "cung", "cũng", "rat tot", "rất tốt"
        };

        for (String indicator : vietnameseIndicators) {
            if (normalizedText.contains(indicator)) {
                logger.debug("Detected Vietnamese by keyword: {}", indicator);
                return "vi";
            }
        }

        // Priority 5: Check for common Vietnamese sentence patterns
        // Vietnamese sentences often start with "Tôi", "Tui", "Mình", "Bạn", etc.
        if (normalizedText.matches("^(toi|tôi|tui|minh|mình|ban|bạn|anh|chi|chị|em|co|cô|ong|ông|ba|bà)\\s+.*")) {
            logger.debug("Detected Vietnamese by sentence pattern");
            return "vi";
        }

        // Priority 6: If text contains only Latin characters and common Vietnamese words, likely Vietnamese
        // Check if text has no Japanese characters but contains Vietnamese-like structure
        if (!hasJapanese && text.matches(".*\\s+.*")) { // Has spaces (common in Vietnamese)
            // Check for Vietnamese word patterns (words separated by spaces)
            String[] words = normalizedText.split("\\s+");
            int vietnameseWordCount = 0;
            for (String word : words) {
                for (String indicator : vietnameseIndicators) {
                    if (word.contains(indicator)) {
                        vietnameseWordCount++;
                        break;
                    }
                }
            }
            // If more than 30% of words match Vietnamese indicators, likely Vietnamese
            if (words.length > 0 && vietnameseWordCount * 100 / words.length >= 30) {
                logger.debug("Detected Vietnamese by word pattern analysis");
                return "vi";
            }
        }

        // Default: If no clear indicators and no Japanese characters, check if it looks like Vietnamese
        // Only assume Vietnamese if there are clear Vietnamese indicators (words, patterns)
        // If no Vietnamese indicators found, reject as unsupported (likely English or other language)
        if (!hasJapanese) {
            // Check if text contains any Vietnamese-like words or patterns
            boolean hasVietnameseWords = false;
            for (String indicator : vietnameseIndicators) {
                if (normalizedText.contains(indicator)) {
                    hasVietnameseWords = true;
                    break;
                }
            }
            
            if (hasVietnameseWords) {
                logger.info("No Japanese characters but has Vietnamese words, assuming Vietnamese");
                return "vi";
            } else {
                // If no clear indicators and no Vietnamese words, likely English or unsupported language
                logger.warn("No clear Vietnamese or Japanese indicators found - likely unsupported language (e.g., English)");
                return "unsupported";
            }
        }

        // Final fallback: assume Japanese (should not reach here if hasJapanese is true)
        logger.warn("Could not detect language, defaulting to Japanese");
        return "ja";
    }

    /**
     * Translate Vietnamese sentence to Japanese appropriate for the user's JLPT level
     * Uses Gemini AI to ensure the translation matches the user's level
     */
    private String translateVietnameseToJapanese(String vietnameseSentence, String level) {
        if (geminiService == null) {
            throw new AIServiceException("Sentence Analysis",
                "Gemini service is required for Vietnamese to Japanese translation",
                "GEMINI_SERVICE_NOT_AVAILABLE");
        }

        // Use Gemini to translate Vietnamese to Japanese appropriate for the level
        String prompt = buildTranslationPrompt(vietnameseSentence, level);
        String jsonResponse = geminiService.generateContent(prompt);
        
        // Extract JSON from response
        jsonResponse = extractJsonFromText(jsonResponse);

        try {
            logger.debug("Parsing Gemini translation response...");
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            Object japaneseSentenceObj = responseMap.get("japanese_sentence");
            
            // Handle case where Gemini returns Map instead of String
            String translatedSentence = null;
            if (japaneseSentenceObj instanceof String) {
                translatedSentence = (String) japaneseSentenceObj;
            } else if (japaneseSentenceObj instanceof Map) {
                // If it's a Map, try to extract text from it
                @SuppressWarnings("unchecked")
                Map<String, Object> sentenceMap = (Map<String, Object>) japaneseSentenceObj;
                Object textObj = sentenceMap.get("text") != null ? sentenceMap.get("text") : 
                                sentenceMap.get("sentence") != null ? sentenceMap.get("sentence") :
                                sentenceMap.values().stream().findFirst().orElse(null);
                if (textObj instanceof String) {
                    translatedSentence = (String) textObj;
                } else if (textObj != null) {
                    translatedSentence = textObj.toString();
                }
            } else if (japaneseSentenceObj != null) {
                translatedSentence = japaneseSentenceObj.toString();
            }
            
            if (translatedSentence == null || translatedSentence.trim().isEmpty()) {
                logger.warn("⚠️ Gemini translation returned empty result, falling back to Google Translate");
                // Fallback to Google Translate if Gemini fails
                return fallbackTranslateVietnameseToJapanese(vietnameseSentence);
            }
            
            String result = translatedSentence.trim();
            logger.info("✅ Gemini translation successful: '{}' -> '{}'", vietnameseSentence, result);
            return result;
        } catch (Exception e) {
            logger.warn("⚠️ Failed to parse translation response from Gemini ({}), falling back to Google Translate: {}", 
                e.getClass().getSimpleName(), e.getMessage());
            // Fallback to Google Translate if parsing fails
            return fallbackTranslateVietnameseToJapanese(vietnameseSentence);
        }
    }

    /**
     * Build prompt for translating Vietnamese to Japanese appropriate for JLPT level
     */
    private String buildTranslationPrompt(String vietnameseSentence, String level) {
        return String.format(
            "You are translating Vietnamese sentences to Japanese for Vietnamese users learning Japanese.\n\n" +
            "Translate this Vietnamese sentence to Japanese: \"%s\"\n\n" +
            "User's JLPT level: %s\n\n" +
            "Requirements:\n" +
            "- Translate to Japanese appropriate for JLPT level %s\n" +
            "- Use vocabulary and grammar patterns that match level %s\n" +
            "- For N5: Use simple vocabulary (hiragana/katakana), basic grammar\n" +
            "- For N4: Can include some common kanji, slightly more complex grammar\n" +
            "- For N3-N1: Use appropriate kanji and grammar patterns for that level\n" +
            "- Keep the meaning accurate and natural\n" +
            "- Return ONLY the Japanese sentence, no explanation\n\n" +
            "Return in JSON format:\n" +
            "{\n" +
            "  \"japanese_sentence\": \"translated Japanese sentence\"\n" +
            "}\n\n" +
            "Return ONLY valid JSON, no additional text",
            vietnameseSentence, level, level, level);
    }

    /**
     * Translate Japanese sentence to Vietnamese
     * Uses Google Translate API (simpler than Vietnamese→Japanese, no level consideration needed)
     */
    private String translateJapaneseToVietnamese(String japaneseSentence) {
        if (aiService == null || !googleCloudEnabled) {
            logger.warn("Translation service not available for Japanese→Vietnamese translation");
            return null; // Return null instead of throwing exception (non-critical)
        }

        try {
            logger.info("Translating Japanese to Vietnamese: '{}'", japaneseSentence);
            Map<String, Object> translationResult = aiService.translateText(japaneseSentence, "ja", "vi");
            String translatedText = (String) translationResult.get("translatedText");
            if (translatedText == null || translatedText.trim().isEmpty()) {
                logger.warn("Google Translate returned empty result for Japanese→Vietnamese");
                return null;
            }
            String result = translatedText.trim();
            logger.info("✅ Japanese→Vietnamese translation successful: '{}' -> '{}'", japaneseSentence, result);
            return result;
        } catch (Exception e) {
            logger.warn("Failed to translate Japanese to Vietnamese (non-critical): {}", e.getMessage());
            return null; // Return null instead of throwing exception (non-critical)
        }
    }

    /**
     * Fallback translation using Google Translate API (without level consideration)
     */
    private String fallbackTranslateVietnameseToJapanese(String vietnameseSentence) {
        if (aiService == null || !googleCloudEnabled) {
            logger.error("❌ Translation service not available - aiService={}, googleCloudEnabled={}", 
                aiService != null, googleCloudEnabled);
            throw new AIServiceException("Sentence Analysis",
                "Translation service is not available. Cannot translate Vietnamese to Japanese.",
                "TRANSLATION_SERVICE_DISABLED");
        }

        try {
            logger.info("Using Google Translate API as fallback for: '{}'", vietnameseSentence);
            Map<String, Object> translationResult = aiService.translateText(vietnameseSentence, "vi", "ja");
            String translatedText = (String) translationResult.get("translatedText");
            if (translatedText == null || translatedText.trim().isEmpty()) {
                logger.error("❌ Google Translate returned empty result");
                throw new AIServiceException("Sentence Analysis",
                    "Translation returned empty result",
                    "TRANSLATION_FAILED");
            }
            String result = translatedText.trim();
            logger.info("✅ Google Translate fallback successful: '{}' -> '{}'", vietnameseSentence, result);
            return result;
        } catch (AIServiceException e) {
            logger.error("❌ Fallback translation failed with AIServiceException", e);
            throw e;
        } catch (Exception e) {
            logger.error("❌ Fallback translation failed with unexpected error", e);
            throw new AIServiceException("Sentence Analysis",
                "Failed to translate Vietnamese to Japanese: " + e.getMessage(), e);
        }
    }
}

