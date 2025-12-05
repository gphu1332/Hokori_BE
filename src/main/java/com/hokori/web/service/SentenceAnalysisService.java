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
            // Step 1: Analyze vocabulary
            List<VocabularyItem> vocabulary = analyzeVocabulary(sentence, normalizedLevel);

            // Step 2: Analyze grammar
            List<GrammarItem> grammar = analyzeGrammar(sentence, normalizedLevel);

            // Step 3: Analyze sentence breakdown
            SentenceAnalysisResponse.SentenceBreakdown breakdown = analyzeSentenceBreakdown(sentence, normalizedLevel);

            // Step 4: Get related sentences
            List<String> relatedSentences = getRelatedSentences(sentence, normalizedLevel);

            // Step 5: Build response
            SentenceAnalysisResponse response = new SentenceAnalysisResponse();
            response.setSentence(sentence);
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
}

