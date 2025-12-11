package com.hokori.web.service;

import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for AI-powered conversation practice
 * Supports multi-turn conversations (6-7 turns) with context awareness
 */
@Service
public class ConversationPracticeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationPracticeService.class);
    
    @Autowired(required = false)
    private GeminiService geminiService;
    
    @Autowired(required = false)
    private AIService aiService;
    
    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;
    
    /**
     * Start a new conversation practice session
     * AI asks the first question based on scenario and level
     */
    public Map<String, Object> startConversation(String level, String scenario) {
        if (!googleCloudEnabled || geminiService == null) {
            throw new AIServiceException("Conversation Practice", 
                "Conversation practice service is not available", 
                "SERVICE_DISABLED");
        }
        
        String normalizedLevel = normalizeLevel(level);
        String normalizedScenario = normalizeScenario(scenario);
        String originalScenario = scenario; // Keep original for display
        
        logger.info("Starting conversation practice: level={}, originalScenario={}, normalizedScenario={}", 
            normalizedLevel, originalScenario, normalizedScenario);
        
        // Build system prompt for conversation (use original scenario if it's a detailed description)
        String systemPrompt = buildSystemPrompt(normalizedLevel, normalizedScenario, originalScenario);
        
        // Generate first question from AI
        String aiQuestion = geminiService.generateConversationResponse(systemPrompt, new ArrayList<>());
        
        if (aiQuestion == null || aiQuestion.trim().isEmpty()) {
            throw new AIServiceException("Conversation Practice", 
                "Failed to generate conversation question", 
                "GENERATION_FAILED");
        }
        
        // Translate to Vietnamese for display
        String aiQuestionVi = translateToVietnamese(aiQuestion);
        
        // Generate audio for AI question
        Map<String, Object> audioResult = generateAudio(aiQuestion);
        
        // Build conversation history
        List<Map<String, String>> conversationHistory = new ArrayList<>();
        Map<String, String> aiMessage = new HashMap<>();
        aiMessage.put("role", "ai");
        aiMessage.put("text", aiQuestion);
        aiMessage.put("textVi", aiQuestionVi);
        conversationHistory.add(aiMessage);
        
        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", generateConversationId()); // Temporary ID
        result.put("level", normalizedLevel);
        result.put("scenario", normalizedScenario); // Normalized scenario key
        result.put("originalScenario", originalScenario); // Original input (Vietnamese or Japanese)
        result.put("aiQuestion", aiQuestion);
        result.put("aiQuestionVi", aiQuestionVi);
        result.put("audioUrl", audioResult.get("audioData"));
        result.put("audioFormat", audioResult.get("audioFormat"));
        result.put("conversationHistory", conversationHistory);
        result.put("turnNumber", 1);
        result.put("maxTurns", 7);
        
        logger.debug("Conversation started successfully: conversationId={}", result.get("conversationId"));
        return result;
    }
    
    /**
     * Respond to conversation and get next AI question
     * FE sends full conversation history, BE processes user audio and generates next question
     */
    public Map<String, Object> respondToConversation(
            String conversationId,
            List<Map<String, String>> conversationHistory,
            String audioData,
            String audioFormat,
            String language,
            String level,
            String scenario) {
        
        if (!googleCloudEnabled || geminiService == null || aiService == null) {
            throw new AIServiceException("Conversation Practice", 
                "Conversation practice service is not available", 
                "SERVICE_DISABLED");
        }
        
        String normalizedLevel = normalizeLevel(level);
        String normalizedScenario = normalizeScenario(scenario);
        String normalizedLanguage = (language != null && !language.isEmpty()) ? language : "ja-JP";
        String normalizedAudioFormat = (audioFormat != null && !audioFormat.isEmpty()) ? audioFormat : "wav";
        
        logger.info("Processing conversation response: conversationId={}, historySize={}, level={}, scenario={}", 
            conversationId, conversationHistory.size(), normalizedLevel, normalizedScenario);
        
        // Step 1: Convert user audio to text
        Map<String, Object> speechToTextResult = aiService.speechToText(audioData, normalizedLanguage, normalizedAudioFormat);
        String userTranscript = (String) speechToTextResult.get("transcript");
        Double confidence = null;
        Object confidenceObj = speechToTextResult.get("confidence");
        if (confidenceObj != null) {
            confidence = ((Number) confidenceObj).doubleValue();
        }
        
        if (userTranscript == null || userTranscript.isEmpty()) {
            throw new AIServiceException("Conversation Practice", 
                "Could not transcribe audio. Please try again with clearer pronunciation.", 
                "TRANSCRIPTION_FAILED");
        }
        
        logger.debug("User transcript: {}, confidence: {}", userTranscript, confidence);
        
        // Step 2: Translate user transcript to Vietnamese for display
        String userTranscriptVi = translateToVietnamese(userTranscript);
        
        // Step 3: Add user message to history
        List<Map<String, String>> updatedHistory = new ArrayList<>(conversationHistory);
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("text", userTranscript);
        userMessage.put("textVi", userTranscriptVi);
        updatedHistory.add(userMessage);
        
        // Step 4: Check if conversation should end (max turns reached)
        int currentTurn = updatedHistory.size() / 2; // Each turn = AI question + user response
        if (currentTurn >= 7) {
            // End conversation
            return buildEndConversationResponse(conversationId, updatedHistory, normalizedLevel, normalizedScenario);
        }
        
        // Step 5: Generate next AI question based on conversation history
        // Use original scenario if available in history metadata, otherwise use normalized
        String originalScenario = scenario; // Could be extracted from history if stored
        String systemPrompt = buildSystemPrompt(normalizedLevel, normalizedScenario, originalScenario);
        
        // Convert conversation history to format expected by Gemini
        List<Map<String, String>> geminiHistory = convertToGeminiHistory(updatedHistory);
        String aiNextQuestion = geminiService.generateConversationResponse(systemPrompt, geminiHistory);
        
        if (aiNextQuestion == null || aiNextQuestion.trim().isEmpty()) {
            throw new AIServiceException("Conversation Practice", 
                "Failed to generate next conversation question", 
                "GENERATION_FAILED");
        }
        
        // Step 6: Translate AI question to Vietnamese
        String aiNextQuestionVi = translateToVietnamese(aiNextQuestion);
        
        // Step 7: Generate audio for AI question
        Map<String, Object> audioResult = generateAudio(aiNextQuestion);
        
        // Step 8: Add AI message to history
        Map<String, String> aiMessage = new HashMap<>();
        aiMessage.put("role", "ai");
        aiMessage.put("text", aiNextQuestion);
        aiMessage.put("textVi", aiNextQuestionVi);
        updatedHistory.add(aiMessage);
        
        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("userTranscript", userTranscript);
        result.put("userTranscriptVi", userTranscriptVi);
        result.put("confidence", confidence);
        result.put("aiNextQuestion", aiNextQuestion);
        result.put("aiNextQuestionVi", aiNextQuestionVi);
        result.put("audioUrl", audioResult.get("audioData"));
        result.put("audioFormat", audioResult.get("audioFormat"));
        result.put("conversationHistory", updatedHistory);
        result.put("turnNumber", currentTurn + 1);
        result.put("maxTurns", 7);
        result.put("isEnding", false);
        
        logger.debug("Conversation response processed: turnNumber={}", result.get("turnNumber"));
        return result;
    }
    
    /**
     * End conversation and get AI evaluation/feedback
     */
    public Map<String, Object> endConversation(
            String conversationId,
            List<Map<String, String>> conversationHistory,
            String level,
            String scenario) {
        
        if (!googleCloudEnabled || geminiService == null) {
            throw new AIServiceException("Conversation Practice", 
                "Conversation practice service is not available", 
                "SERVICE_DISABLED");
        }
        
        String normalizedLevel = normalizeLevel(level);
        String normalizedScenario = normalizeScenario(scenario);
        
        logger.info("Ending conversation: conversationId={}, historySize={}, level={}, scenario={}", 
            conversationId, conversationHistory.size(), normalizedLevel, normalizedScenario);
        
        // Build evaluation prompt
        String evaluationPrompt = buildEvaluationPrompt(normalizedLevel, normalizedScenario, conversationHistory);
        
        // Generate evaluation
        String evaluationJson = geminiService.generateContent(evaluationPrompt);
        
        // Parse evaluation (simplified - in production, use proper JSON parsing)
        Map<String, Object> evaluation = parseEvaluation(evaluationJson);
        
        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("level", normalizedLevel);
        result.put("scenario", normalizedScenario);
        result.put("fullConversation", conversationHistory);
        result.put("evaluation", evaluation);
        result.put("turnNumber", conversationHistory.size() / 2);
        
        logger.debug("Conversation ended: conversationId={}", conversationId);
        return result;
    }
    
    /**
     * Build system prompt for conversation based on level and scenario
     * @param originalScenario Original user input (can be a detailed description)
     */
    private String buildSystemPrompt(String level, String scenario, String originalScenario) {
        String levelDescription = getLevelDescription(level);
        
        // Check if originalScenario is a detailed description (long sentence)
        boolean isDetailedDescription = isDetailedScenarioDescription(originalScenario);
        
        String scenarioContext;
        if (isDetailedDescription) {
            // Use original detailed description for context
            scenarioContext = String.format(
                "Detailed scenario description: %s. " +
                "This is a specific situation the user wants to practice. " +
                "Create a conversation that helps the user practice this situation.",
                originalScenario
            );
        } else {
            // Use standard scenario description
            String scenarioDescription = getScenarioDescription(scenario);
            scenarioContext = String.format("Scenario: %s.", scenarioDescription);
        }
        
        return String.format(
            "You are a friendly Japanese conversation partner for Vietnamese learners. " +
            "Your role: Ask questions and continue natural conversations in Japanese. " +
            "%s " +
            "User level: %s. " +
            "Rules: " +
            "1. Speak only in Japanese (no Vietnamese, no English). " +
            "2. Use vocabulary and grammar appropriate for %s level. " +
            "3. Keep questions and responses short (1-2 sentences, max 50 characters). " +
            "4. Be natural and friendly. " +
            "5. Continue the conversation based on user's previous responses. " +
            "6. Ask follow-up questions related to the scenario. " +
            "7. If the scenario is a specific situation (like calling police, emergency, etc.), " +
            "   act as the appropriate person (police officer, shopkeeper, etc.) and guide the conversation naturally. " +
            "Now, start the conversation with an appropriate greeting or question for this scenario.",
            scenarioContext, levelDescription, level
        );
    }
    
    /**
     * Check if scenario input is a detailed description (long sentence) rather than a simple keyword
     */
    private boolean isDetailedScenarioDescription(String scenario) {
        if (scenario == null || scenario.isEmpty()) {
            return false;
        }
        
        // If it's longer than 20 characters, likely a detailed description
        if (scenario.length() > 20) {
            return true;
        }
        
        // Check if it contains common Vietnamese sentence patterns
        String lowerScenario = scenario.toLowerCase();
        String[] sentenceIndicators = {
            "đang", "tôi", "toi", "tui", "mình", "minh",
            "nên", "nen", "cần", "can", "muốn", "muon",
            "và", "va", "hoặc", "hoac", "với", "voi",
            "gọi", "goi", "nói", "noi", "hỏi", "hoi"
        };
        
        int indicatorCount = 0;
        for (String indicator : sentenceIndicators) {
            if (lowerScenario.contains(indicator)) {
                indicatorCount++;
            }
        }
        
        // If contains 2+ sentence indicators, likely a detailed description
        return indicatorCount >= 2;
    }
    
    /**
     * Build evaluation prompt
     */
    private String buildEvaluationPrompt(String level, String scenario, List<Map<String, String>> conversationHistory) {
        StringBuilder historyText = new StringBuilder();
        for (Map<String, String> message : conversationHistory) {
            String role = message.get("role");
            String text = message.get("text");
            if ("user".equals(role)) {
                historyText.append("User: ").append(text).append("\n");
            } else if ("ai".equals(role)) {
                historyText.append("AI: ").append(text).append("\n");
            }
        }
        
        return String.format(
            "Evaluate this Japanese conversation practice session. " +
            "User level: %s. Scenario: %s. " +
            "Conversation:\n%s\n" +
            "Provide evaluation in JSON format: " +
            "{\"overallScore\": 0-100, \"accuracyScore\": 0-100, \"fluencyScore\": 0-100, " +
            "\"grammarScore\": 0-100, \"vocabularyScore\": 0-100, " +
            "\"overallFeedbackVi\": \"...\", \"strengthsVi\": [\"...\"], \"improvementsVi\": [\"...\"], " +
            "\"suggestionsVi\": [\"...\"]}. " +
            "All feedback must be in Vietnamese.",
            level, scenario, historyText.toString()
        );
    }
    
    /**
     * Parse evaluation JSON response
     */
    private Map<String, Object> parseEvaluation(String evaluationJson) {
        Map<String, Object> evaluation = new HashMap<>();
        
        // Simple parsing (in production, use proper JSON parsing)
        try {
            // Try to extract JSON from markdown if present
            String jsonText = evaluationJson;
            if (evaluationJson.contains("```json")) {
                int start = evaluationJson.indexOf("```json") + 7;
                int end = evaluationJson.indexOf("```", start);
                if (end > start) {
                    jsonText = evaluationJson.substring(start, end).trim();
                }
            } else if (evaluationJson.contains("```")) {
                int start = evaluationJson.indexOf("```") + 3;
                int end = evaluationJson.indexOf("```", start);
                if (end > start) {
                    jsonText = evaluationJson.substring(start, end).trim();
                }
            }
            
            // Use GeminiService's JSON parsing if available
            if (geminiService != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode jsonNode = geminiService.generateContentAsJson(
                        "Parse this JSON: " + jsonText
                    );
                    if (jsonNode != null) {
                        // Extract fields from JSON node
                        if (jsonNode.has("overallScore")) {
                            evaluation.put("overallScore", jsonNode.get("overallScore").asDouble());
                        }
                        if (jsonNode.has("accuracyScore")) {
                            evaluation.put("accuracyScore", jsonNode.get("accuracyScore").asDouble());
                        }
                        if (jsonNode.has("fluencyScore")) {
                            evaluation.put("fluencyScore", jsonNode.get("fluencyScore").asDouble());
                        }
                        if (jsonNode.has("grammarScore")) {
                            evaluation.put("grammarScore", jsonNode.get("grammarScore").asDouble());
                        }
                        if (jsonNode.has("vocabularyScore")) {
                            evaluation.put("vocabularyScore", jsonNode.get("vocabularyScore").asDouble());
                        }
                        if (jsonNode.has("overallFeedbackVi")) {
                            evaluation.put("overallFeedbackVi", jsonNode.get("overallFeedbackVi").asText());
                        }
                        if (jsonNode.has("strengthsVi")) {
                            List<String> strengths = new ArrayList<>();
                            jsonNode.get("strengthsVi").forEach(node -> strengths.add(node.asText()));
                            evaluation.put("strengthsVi", strengths);
                        }
                        if (jsonNode.has("improvementsVi")) {
                            List<String> improvements = new ArrayList<>();
                            jsonNode.get("improvementsVi").forEach(node -> improvements.add(node.asText()));
                            evaluation.put("improvementsVi", improvements);
                        }
                        if (jsonNode.has("suggestionsVi")) {
                            List<String> suggestions = new ArrayList<>();
                            jsonNode.get("suggestionsVi").forEach(node -> suggestions.add(node.asText()));
                            evaluation.put("suggestionsVi", suggestions);
                        }
                        return evaluation;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse evaluation JSON, using fallback", e);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse evaluation JSON", e);
        }
        
        // Fallback: return basic evaluation
        evaluation.put("overallScore", 75.0);
        evaluation.put("overallFeedbackVi", "Cuộc trò chuyện đã hoàn thành. Hãy tiếp tục luyện tập để cải thiện kỹ năng!");
        evaluation.put("strengthsVi", Arrays.asList("Đã hoàn thành cuộc trò chuyện"));
        evaluation.put("improvementsVi", Arrays.asList("Tiếp tục luyện tập phát âm và từ vựng"));
        evaluation.put("suggestionsVi", Arrays.asList("Thử các tình huống khác nhau để mở rộng vốn từ"));
        
        return evaluation;
    }
    
    /**
     * Convert conversation history to Gemini format
     */
    private List<Map<String, String>> convertToGeminiHistory(List<Map<String, String>> conversationHistory) {
        List<Map<String, String>> geminiHistory = new ArrayList<>();
        for (Map<String, String> message : conversationHistory) {
            Map<String, String> geminiMessage = new HashMap<>();
            String role = message.get("role");
            geminiMessage.put("role", "user".equals(role) ? "user" : "model");
            geminiMessage.put("text", message.get("text"));
            geminiHistory.add(geminiMessage);
        }
        return geminiHistory;
    }
    
    /**
     * Build end conversation response
     */
    private Map<String, Object> buildEndConversationResponse(
            String conversationId,
            List<Map<String, String>> conversationHistory,
            String level,
            String scenario) {
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("fullConversation", conversationHistory);
        result.put("level", level);
        result.put("scenario", scenario);
        result.put("turnNumber", conversationHistory.size() / 2);
        result.put("isEnding", true);
        result.put("message", "Cuộc trò chuyện đã đạt số lượt tối đa. Vui lòng kết thúc để xem đánh giá.");
        return result;
    }
    
    /**
     * Translate Japanese text to Vietnamese
     */
    private String translateToVietnamese(String japaneseText) {
        if (aiService == null || japaneseText == null || japaneseText.trim().isEmpty()) {
            return "";
        }
        
        try {
            Map<String, Object> translationResult = aiService.translateText(japaneseText, "ja", "vi");
            String translated = (String) translationResult.get("translatedText");
            return translated != null ? translated : "";
        } catch (Exception e) {
            logger.warn("Failed to translate to Vietnamese: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Generate audio from text
     */
    private Map<String, Object> generateAudio(String text) {
        if (aiService == null) {
            return new HashMap<>();
        }
        
        try {
            return aiService.textToSpeech(text, "ja-JP-Standard-A", "normal");
        } catch (Exception e) {
            logger.warn("Failed to generate audio: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Generate temporary conversation ID
     */
    private String generateConversationId() {
        return "conv-" + UUID.randomUUID().toString().substring(0, 8);
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
        return "N5";
    }
    
    /**
     * Normalize scenario - detect language and map to scenario key
     * Supports Vietnamese and Japanese input, including detailed descriptions
     */
    private String normalizeScenario(String scenario) {
        if (scenario == null || scenario.isEmpty()) {
            return "greeting";
        }
        
        String normalized = scenario.trim();
        
        // Check if it's a detailed description (long sentence)
        // If so, keep it as-is and use a generic key, the detailed description will be used in system prompt
        if (isDetailedScenarioDescription(normalized)) {
            // Return a generic key, but the original will be preserved and used in system prompt
            return "custom"; // Generic key for custom scenarios
        }
        
        // First, try to match against known scenario keys (English)
        String lowerScenario = normalized.toLowerCase();
        if (isKnownScenarioKey(lowerScenario)) {
            return lowerScenario;
        }
        
        // Detect language
        String detectedLanguage = detectScenarioLanguage(normalized);
        
        // Map Vietnamese scenarios to keys
        if ("vi".equals(detectedLanguage)) {
            String mappedKey = mapVietnameseToScenarioKey(normalized);
            if (mappedKey != null) {
                return mappedKey;
            }
            // If not mapped and not a detailed description, use Gemini to understand
            if (normalized.length() <= 50) { // Short enough to try mapping
                return understandScenarioFromVietnamese(normalized);
            }
            // Long description, use generic key
            return "custom";
        }
        
        // Map Japanese scenarios to keys
        if ("ja".equals(detectedLanguage)) {
            String mappedKey = mapJapaneseToScenarioKey(normalized);
            if (mappedKey != null) {
                return mappedKey;
            }
            // If not mapped and not a detailed description, use Gemini to understand
            if (normalized.length() <= 50) { // Short enough to try mapping
                return understandScenarioFromJapanese(normalized);
            }
            // Long description, use generic key
            return "custom";
        }
        
        // Default fallback
        return "greeting";
    }
    
    /**
     * Check if scenario is a known key
     */
    private boolean isKnownScenarioKey(String scenario) {
        return scenario.equals("restaurant") || 
               scenario.equals("shopping") || 
               scenario.equals("greeting") || 
               scenario.equals("directions") || 
               scenario.equals("hotel") || 
               scenario.equals("airport");
    }
    
    /**
     * Detect language of scenario input
     */
    private String detectScenarioLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        
        // Check for Japanese characters
        if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) {
            return "ja";
        }
        
        // Check for Vietnamese diacritics
        if (text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ].*")) {
            return "vi";
        }
        
        // Check for common Vietnamese scenario keywords
        String lowerText = text.toLowerCase();
        String[] vietnameseKeywords = {
            "nhà hàng", "nha hang", "quán ăn", "quan an",
            "mua sắm", "mua sam", "cửa hàng", "cua hang", "siêu thị", "sieu thi",
            "chào hỏi", "chao hoi", "giới thiệu", "gioi thieu",
            "hỏi đường", "hoi duong", "chỉ đường", "chi duong",
            "khách sạn", "khach san", "lễ tân", "le tan",
            "sân bay", "san bay", "máy bay", "may bay"
        };
        for (String keyword : vietnameseKeywords) {
            if (lowerText.contains(keyword)) {
                return "vi";
            }
        }
        
        // Check for common Japanese scenario keywords
        String[] japaneseKeywords = {
            "レストラン", "食堂", "食事",
            "ショッピング", "買い物", "店",
            "挨拶", "紹介", "初めまして",
            "道", "方向", "案内",
            "ホテル", "宿泊",
            "空港", "飛行機"
        };
        for (String keyword : japaneseKeywords) {
            if (text.contains(keyword)) {
                return "ja";
            }
        }
        
        // Default: assume Vietnamese if contains common Vietnamese words
        String[] commonVietnameseWords = {"tôi", "toi", "bạn", "ban", "của", "cua", "và", "va"};
        for (String word : commonVietnameseWords) {
            if (lowerText.contains(word)) {
                return "vi";
            }
        }
        
        return "unknown";
    }
    
    /**
     * Map Vietnamese scenario text to scenario key
     */
    private String mapVietnameseToScenarioKey(String vietnameseText) {
        String lowerText = vietnameseText.toLowerCase();
        
        // Restaurant scenarios
        if (lowerText.contains("nhà hàng") || lowerText.contains("nha hang") || 
            lowerText.contains("quán ăn") || lowerText.contains("quan an") ||
            lowerText.contains("ăn uống") || lowerText.contains("an uong") ||
            lowerText.contains("thức ăn") || lowerText.contains("thuc an")) {
            return "restaurant";
        }
        
        // Shopping scenarios
        if (lowerText.contains("mua sắm") || lowerText.contains("mua sam") ||
            lowerText.contains("cửa hàng") || lowerText.contains("cua hang") ||
            lowerText.contains("siêu thị") || lowerText.contains("sieu thi") ||
            lowerText.contains("mua đồ") || lowerText.contains("mua do")) {
            return "shopping";
        }
        
        // Greeting scenarios
        if (lowerText.contains("chào hỏi") || lowerText.contains("chao hoi") ||
            lowerText.contains("giới thiệu") || lowerText.contains("gioi thieu") ||
            lowerText.contains("làm quen") || lowerText.contains("lam quen")) {
            return "greeting";
        }
        
        // Directions scenarios
        if (lowerText.contains("hỏi đường") || lowerText.contains("hoi duong") ||
            lowerText.contains("chỉ đường") || lowerText.contains("chi duong") ||
            lowerText.contains("địa chỉ") || lowerText.contains("dia chi")) {
            return "directions";
        }
        
        // Hotel scenarios
        if (lowerText.contains("khách sạn") || lowerText.contains("khach san") ||
            lowerText.contains("lễ tân") || lowerText.contains("le tan") ||
            lowerText.contains("đặt phòng") || lowerText.contains("dat phong")) {
            return "hotel";
        }
        
        // Airport scenarios
        if (lowerText.contains("sân bay") || lowerText.contains("san bay") ||
            lowerText.contains("máy bay") || lowerText.contains("may bay") ||
            lowerText.contains("check-in") || lowerText.contains("checkin")) {
            return "airport";
        }
        
        return null;
    }
    
    /**
     * Map Japanese scenario text to scenario key
     */
    private String mapJapaneseToScenarioKey(String japaneseText) {
        // Restaurant scenarios
        if (japaneseText.contains("レストラン") || japaneseText.contains("食堂") || 
            japaneseText.contains("食事") || japaneseText.contains("料理")) {
            return "restaurant";
        }
        
        // Shopping scenarios
        if (japaneseText.contains("ショッピング") || japaneseText.contains("買い物") ||
            japaneseText.contains("店") || japaneseText.contains("デパート")) {
            return "shopping";
        }
        
        // Greeting scenarios
        if (japaneseText.contains("挨拶") || japaneseText.contains("紹介") ||
            japaneseText.contains("初めまして") || japaneseText.contains("自己紹介")) {
            return "greeting";
        }
        
        // Directions scenarios
        if (japaneseText.contains("道") || japaneseText.contains("方向") ||
            japaneseText.contains("案内") || japaneseText.contains("場所")) {
            return "directions";
        }
        
        // Hotel scenarios
        if (japaneseText.contains("ホテル") || japaneseText.contains("宿泊") ||
            japaneseText.contains("チェックイン")) {
            return "hotel";
        }
        
        // Airport scenarios
        if (japaneseText.contains("空港") || japaneseText.contains("飛行機") ||
            japaneseText.contains("搭乗")) {
            return "airport";
        }
        
        return null;
    }
    
    /**
     * Use Gemini to understand scenario from Vietnamese input
     */
    private String understandScenarioFromVietnamese(String vietnameseText) {
        if (geminiService == null) {
            return "greeting"; // Fallback
        }
        
        try {
            String prompt = String.format(
                "Phân loại tình huống trò chuyện sau đây vào một trong các loại: restaurant, shopping, greeting, directions, hotel, airport. " +
                "Chỉ trả về một từ khóa duy nhất (restaurant, shopping, greeting, directions, hotel, hoặc airport). " +
                "Tình huống: %s",
                vietnameseText
            );
            
            String response = geminiService.generateContent(prompt);
            if (response != null) {
                String normalized = response.trim().toLowerCase();
                if (isKnownScenarioKey(normalized)) {
                    return normalized;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to understand scenario from Vietnamese: {}", e.getMessage());
        }
        
        return "greeting"; // Fallback
    }
    
    /**
     * Use Gemini to understand scenario from Japanese input
     */
    private String understandScenarioFromJapanese(String japaneseText) {
        if (geminiService == null) {
            return "greeting"; // Fallback
        }
        
        try {
            String prompt = String.format(
                "以下の会話シナリオを分類してください: restaurant, shopping, greeting, directions, hotel, airport. " +
                "1つのキーワードのみを返してください (restaurant, shopping, greeting, directions, hotel, または airport). " +
                "シナリオ: %s",
                japaneseText
            );
            
            String response = geminiService.generateContent(prompt);
            if (response != null) {
                String normalized = response.trim().toLowerCase();
                if (isKnownScenarioKey(normalized)) {
                    return normalized;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to understand scenario from Japanese: {}", e.getMessage());
        }
        
        return "greeting"; // Fallback
    }
    
    /**
     * Get scenario description
     */
    private String getScenarioDescription(String scenario) {
        Map<String, String> scenarios = new HashMap<>();
        scenarios.put("restaurant", "nhà hàng (ordering food, asking about menu)");
        scenarios.put("shopping", "mua sắm (asking about products, prices)");
        scenarios.put("greeting", "chào hỏi (introductions, small talk)");
        scenarios.put("directions", "hỏi đường (asking for directions)");
        scenarios.put("hotel", "khách sạn (check-in, asking about facilities)");
        scenarios.put("airport", "sân bay (check-in, security, boarding)");
        scenarios.put("custom", "tình huống tùy chỉnh (custom situation)");
        return scenarios.getOrDefault(scenario, "cuộc trò chuyện thông thường");
    }
    
    /**
     * Get level description
     */
    private String getLevelDescription(String level) {
        Map<String, String> levels = new HashMap<>();
        levels.put("N5", "beginner (basic vocabulary and grammar)");
        levels.put("N4", "elementary (simple daily conversations)");
        levels.put("N3", "intermediate (everyday situations)");
        levels.put("N2", "upper-intermediate (complex conversations)");
        levels.put("N1", "advanced (fluent conversations)");
        return levels.getOrDefault(level, "beginner");
    }
}

