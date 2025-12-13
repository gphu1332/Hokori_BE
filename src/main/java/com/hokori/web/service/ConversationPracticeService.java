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
        
        // Validate audio data before processing
        if (audioData == null || audioData.trim().isEmpty()) {
            throw new AIServiceException("Conversation Practice", 
                "Audio data is empty. Please record your response before submitting.", 
                "EMPTY_AUDIO");
        }
        
        // Check minimum audio data length (base64 string should be at least 500 chars for valid audio)
        String trimmedAudio = audioData.trim();
        if (trimmedAudio.length() < 500) {
            throw new AIServiceException("Conversation Practice", 
                "Audio recording is too short or empty. Please record your response (at least 1-2 seconds) before submitting.", 
                "AUDIO_TOO_SHORT");
        }
        
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
                "Could not transcribe audio. The recording may be empty, too quiet, or unclear. Please try again with clearer pronunciation.", 
                "TRANSCRIPTION_FAILED");
        }
        
        // Additional check: if transcript is too short (just noise or silence)
        if (userTranscript.trim().length() < 2) {
            throw new AIServiceException("Conversation Practice", 
                "Could not detect any speech in the recording. Please speak clearly and try again.", 
                "NO_SPEECH_DETECTED");
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
     * Build enhanced system prompt for conversation based on level and scenario
     * Improved prompt for better conversation quality and learning outcomes
     * @param originalScenario Original user input (can be a detailed description)
     */
    private String buildSystemPrompt(String level, String scenario, String originalScenario) {
        String levelDescription = getLevelDescription(level);
        String levelGuidelines = getLevelSpecificGuidelines(level);
        
        // Check if originalScenario is a detailed description (long sentence)
        boolean isDetailedDescription = isDetailedScenarioDescription(originalScenario);
        
        String scenarioContext;
        if (isDetailedDescription) {
            // Use original detailed description for context
            scenarioContext = String.format(
                "**Tình huống Chi tiết:** %s\n" +
                "Đây là một tình huống cụ thể mà người dùng muốn luyện tập. " +
                "Hãy tạo một cuộc trò chuyện thực tế, hữu ích hướng dẫn người dùng qua tình huống này một cách tự nhiên.",
                originalScenario
            );
        } else {
            // Use standard scenario description
            String scenarioDescription = getScenarioDescription(scenario);
            scenarioContext = String.format("**Tình huống:** %s", scenarioDescription);
        }
        
        return String.format(
            "Bạn là một đối tác trò chuyện tiếng Nhật chuyên nghiệp, kiên nhẫn và khuyến khích dành cho người Việt học tiếng Nhật. " +
            "Mục tiêu của bạn là giúp người dùng luyện tập tiếng Nhật một cách tự nhiên trong môi trường học tập hỗ trợ.\n\n" +
            
            "**Ngữ cảnh:**\n" +
            "%s\n" +
            "**Trình độ người dùng:** %s (%s)\n\n" +
            
            "**Vai trò & Tính cách của bạn:**\n" +
            "- Đóng vai người Nhật bản xứ phù hợp với tình huống này (nhân viên cửa hàng, phục vụ, bạn bè, v.v.)\n" +
            "- Hãy ấm áp, kiên nhẫn và khuyến khích - như một giáo viên hữu ích hoặc người địa phương thân thiện\n" +
            "- Thể hiện sự quan tâm đến phản hồi của người dùng và phát triển dựa trên những gì họ nói\n" +
            "- Sử dụng các biểu cảm tiếng Nhật tự nhiên và cách nói lịch sự phù hợp với tình huống\n" +
            "- Phản ứng tự nhiên với phản hồi của người dùng (thể hiện ngạc nhiên, yêu cầu làm rõ, thể hiện sự hiểu biết)\n\n" +
            
            "**Hướng dẫn Ngôn ngữ:**\n" +
            "%s\n\n" +
            
            "**Quy tắc Trò chuyện:**\n" +
            "1. **Chỉ dùng tiếng Nhật:** Chỉ nói bằng tiếng Nhật. KHÔNG BAO GIỜ dùng tiếng Việt, tiếng Anh hay bất kỳ ngôn ngữ nào khác.\n" +
            "2. **Kiểm soát độ dài:** Giữ phản hồi ngắn gọn:\n" +
            "   - N5/N4: 1 câu, tối đa 30 ký tự\n" +
            "   - N3: 1-2 câu, tối đa 50 ký tự\n" +
            "   - N2/N1: 2-3 câu, tối đa 80 ký tự\n" +
            "3. **Luồng tự nhiên:** Làm cho cuộc trò chuyện cảm thấy tự nhiên và thực tế:\n" +
            "   - Bắt đầu bằng lời chào phù hợp với tình huống\n" +
            "   - Đặt câu hỏi tiếp theo dựa trên phản hồi của người dùng\n" +
            "   - Thể hiện sự quan tâm và tham gia (\"そうですか\", \"いいですね\", \"なるほど\")\n" +
            "   - Sử dụng các phản ứng và từ cảm thán phù hợp\n" +
            "4. **Độ khó thích ứng:**\n" +
            "   - Nếu người dùng gặp khó khăn: Đơn giản hóa ngôn ngữ, dùng từ vựng dễ hơn\n" +
            "   - Nếu người dùng làm tốt: Dần dần giới thiệu các cách diễn đạt phức tạp hơn một chút\n" +
            "   - Luôn giữ trong phạm vi trình độ %s\n" +
            "5. **Nhận thức ngữ cảnh:**\n" +
            "   - Nhớ những gì người dùng đã nói ở các lượt trước\n" +
            "   - Tham chiếu tự nhiên đến các phần trước của cuộc trò chuyện\n" +
            "   - Phát triển chủ đề trò chuyện một cách tiến bộ\n" +
            "6. **Xử lý lỗi:**\n" +
            "   - Nếu người dùng mắc lỗi: Tiếp tục tự nhiên, không sửa lỗi trực tiếp\n" +
            "   - Sử dụng tiếng Nhật đúng trong phản hồi của bạn để làm mẫu cách sử dụng đúng\n" +
            "   - Nếu không rõ: Hỏi lại một cách lịch sự (\"すみません、もう一度お願いします\")\n" +
            "7. **Hành vi theo Tình huống:**\n" +
            "   - Hành động phù hợp với tình huống (trang trọng ở nhà hàng, thân mật với bạn bè, v.v.)\n" +
            "   - Sử dụng từ vựng và cách diễn đạt phù hợp với tình huống\n" +
            "   - Hướng dẫn cuộc trò chuyện hướng tới mục tiêu tình huống một cách tự nhiên\n\n" +
            
            "**Phong cách Phản hồi:**\n" +
            "- Hãy trò chuyện tự nhiên, không máy móc\n" +
            "- Thay đổi câu hỏi và phản hồi của bạn (đừng lặp lại cùng một cấu trúc)\n" +
            "- Sử dụng các mẫu trò chuyện tiếng Nhật tự nhiên và từ đệm khi phù hợp\n" +
            "- Thể hiện tính cách trong khi vẫn giữ chuyên nghiệp\n\n" +
            
            "**Quan trọng:**\n" +
            "- Đây là buổi LUYỆN TẬP - ưu tiên học tập hơn sự hoàn hảo\n" +
            "- Làm cho người dùng cảm thấy thoải mái và tự tin\n" +
            "- Tạo bầu không khí tích cực, khuyến khích\n" +
            "- Tập trung vào tiếng Nhật thực tế, ứng dụng trong đời sống\n\n" +
            
            "Bây giờ, hãy bắt đầu cuộc trò chuyện với một mở đầu phù hợp, tự nhiên cho tình huống này. " +
            "Hãy làm cho nó cảm thấy như một cuộc trò chuyện thật, không phải một bài kiểm tra.",
            scenarioContext, level, levelDescription, levelGuidelines, level
        );
    }
    
    /**
     * Get level-specific language guidelines for the prompt (in Vietnamese)
     */
    private String getLevelSpecificGuidelines(String level) {
        switch (level.toUpperCase()) {
            case "N5":
                return "- Chỉ sử dụng từ vựng cơ bản (hiragana, katakana, kanji đơn giản)\n" +
                       "- Cấu trúc câu đơn giản (Chủ ngữ-Tân ngữ-Động từ)\n" +
                       "- Cách nói lịch sự cơ bản (です/ます)\n" +
                       "- Chỉ các biểu cảm hàng ngày thông thường\n" +
                       "- Tránh ngữ pháp phức tạp (không có điều kiện, không có bị động, không có sai khiến)";
            
            case "N4":
                return "- Từ vựng sơ cấp với một số kanji thông dụng\n" +
                       "- Mẫu câu đơn giản với các liên từ cơ bản (そして, でも, から)\n" +
                       "- Cách nói lịch sự (です/ます) và một số cách nói thân mật\n" +
                       "- Biểu cảm thời gian cơ bản và điều kiện đơn giản (たら, と)\n" +
                       "- Tránh các cấu trúc ngữ pháp nâng cao";
            
            case "N3":
                return "- Từ vựng trung cấp với kanji thông dụng\n" +
                       "- Cấu trúc câu phức tạp hơn\n" +
                       "- Kết hợp cách nói lịch sự và thân mật dựa trên ngữ cảnh\n" +
                       "- Các dạng điều kiện (ば, たら, と, なら)\n" +
                       "- Dạng bị động và sai khiến thỉnh thoảng\n" +
                       "- Mẫu trò chuyện tự nhiên";
            
            case "N2":
                return "- Từ vựng trung cấp cao với kanji đa dạng\n" +
                       "- Cấu trúc câu phức tạp với nhiều mệnh đề\n" +
                       "- Mức độ trang trọng phù hợp cho các tình huống khác nhau\n" +
                       "- Mẫu ngữ pháp nâng cao (bị động, sai khiến, kính ngữ)\n" +
                       "- Cách diễn đạt tự nhiên, tinh tế\n" +
                       "- Thành ngữ khi phù hợp";
            
            case "N1":
                return "- Từ vựng nâng cao với kanji phức tạp\n" +
                       "- Cấu trúc câu tinh vi\n" +
                       "- Thành thạo các mức độ trang trọng và kính ngữ\n" +
                       "- Mẫu ngữ pháp phức tạp và cách diễn đạt tinh tế\n" +
                       "- Trò chuyện tự nhiên, trôi chảy với nhận thức văn hóa\n" +
                       "- Thành ngữ và cách nói thông tục nâng cao";
            
            default:
                return "- Sử dụng từ vựng và ngữ pháp phù hợp với trình độ sơ cấp\n" +
                       "- Câu đơn giản, rõ ràng";
        }
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
     * Build evaluation prompt with detailed analysis
     */
    private String buildEvaluationPrompt(String level, String scenario, List<Map<String, String>> conversationHistory) {
        StringBuilder historyText = new StringBuilder();
        int turnNumber = 1;
        for (Map<String, String> message : conversationHistory) {
            String role = message.get("role");
            String text = message.get("text");
            if ("ai".equals(role)) {
                historyText.append(String.format("[Turn %d] AI: %s\n", turnNumber, text));
            } else if ("user".equals(role)) {
                historyText.append(String.format("[Turn %d] User: %s\n", turnNumber, text));
                turnNumber++;
            }
        }
        
        String levelDescription = getLevelDescription(level);
        String scenarioDescription = getScenarioDescription(scenario);
        
        return String.format(
            "Bạn là giáo viên tiếng Nhật chuyên nghiệp. Hãy đánh giá chi tiết cuộc trò chuyện thực hành tiếng Nhật sau đây.\n\n" +
            "**Thông tin:**\n" +
            "- Trình độ học viên: %s (%s)\n" +
            "- Tình huống: %s\n" +
            "- Số lượt trò chuyện: %d\n\n" +
            "**Cuộc trò chuyện:**\n%s\n\n" +
            "**Yêu cầu đánh giá:**\n" +
            "1. **Điểm số (0-100):**\n" +
            "   - overallScore: Tổng điểm tổng thể\n" +
            "   - accuracyScore: Độ chính xác (ngữ pháp, từ vựng, cách diễn đạt)\n" +
            "   - fluencyScore: Độ trôi chảy (tốc độ, nhịp điệu, tự nhiên)\n" +
            "   - grammarScore: Ngữ pháp (cấu trúc câu, chia động từ, trợ từ)\n" +
            "   - vocabularyScore: Từ vựng (sử dụng từ phù hợp, đa dạng)\n\n" +
            "2. **Phân tích chi tiết:**\n" +
            "   - overallFeedbackVi: Nhận xét tổng quan về toàn bộ cuộc trò chuyện (2-3 câu)\n" +
            "   - strengthsVi: Mảng 3-5 điểm mạnh cụ thể (ví dụ: \"Sử dụng đúng trợ từ を trong câu 'りんごを食べます'\", \"Phát âm rõ ràng các từ khó\")\n" +
            "   - improvementsVi: Mảng 3-5 điểm cần cải thiện cụ thể với ví dụ (ví dụ: \"Lỗi chia động từ: '食べる' nên là '食べます' trong ngữ cảnh lịch sự\", \"Thiếu trợ từ に khi nói về địa điểm\")\n" +
            "   - suggestionsVi: Mảng 3-5 gợi ý cụ thể để cải thiện (ví dụ: \"Luyện tập thêm cách sử dụng trợ từ に và で\", \"Học thêm từ vựng về chủ đề nhà hàng\")\n" +
            "   - detailedAnalysisVi: Phân tích chi tiết từng lượt trả lời của học viên, chỉ ra lỗi cụ thể và cách sửa (mảng các object với format: {\"turn\": số lượt, \"userResponse\": \"câu trả lời của học viên\", \"errors\": [\"lỗi 1\", \"lỗi 2\"], \"corrections\": [\"cách sửa 1\", \"cách sửa 2\"], \"betterResponse\": \"câu trả lời tốt hơn\"})\n\n" +
            "**Lưu ý:**\n" +
            "- Đánh giá dựa trên trình độ %s, không quá khắt khe nhưng cũng không quá dễ dãi\n" +
            "- Chỉ ra lỗi cụ thể với ví dụ từ cuộc trò chuyện\n" +
            "- Đưa ra gợi ý thực tế, có thể áp dụng ngay\n" +
            "- Tất cả feedback phải bằng tiếng Việt\n" +
            "- Phân tích chi tiết phải cụ thể, không chung chung\n\n" +
            "Trả về kết quả dưới dạng JSON hợp lệ:\n" +
            "{\n" +
            "  \"overallScore\": số (0-100),\n" +
            "  \"accuracyScore\": số (0-100),\n" +
            "  \"fluencyScore\": số (0-100),\n" +
            "  \"grammarScore\": số (0-100),\n" +
            "  \"vocabularyScore\": số (0-100),\n" +
            "  \"overallFeedbackVi\": \"chuỗi\",\n" +
            "  \"strengthsVi\": [\"chuỗi\", \"chuỗi\", ...],\n" +
            "  \"improvementsVi\": [\"chuỗi\", \"chuỗi\", ...],\n" +
            "  \"suggestionsVi\": [\"chuỗi\", \"chuỗi\", ...],\n" +
            "  \"detailedAnalysisVi\": [\n" +
            "    {\"turn\": số, \"userResponse\": \"chuỗi\", \"errors\": [\"chuỗi\"], \"corrections\": [\"chuỗi\"], \"betterResponse\": \"chuỗi\"},\n" +
            "    ...\n" +
            "  ]\n" +
            "}",
            level, levelDescription, scenarioDescription, turnNumber - 1, historyText.toString(), level
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
                        if (jsonNode.has("detailedAnalysisVi")) {
                            List<Map<String, Object>> detailedAnalysis = new ArrayList<>();
                            jsonNode.get("detailedAnalysisVi").forEach(node -> {
                                Map<String, Object> turnAnalysis = new HashMap<>();
                                if (node.has("turn")) {
                                    turnAnalysis.put("turn", node.get("turn").asInt());
                                }
                                if (node.has("userResponse")) {
                                    turnAnalysis.put("userResponse", node.get("userResponse").asText());
                                }
                                if (node.has("errors")) {
                                    List<String> errors = new ArrayList<>();
                                    node.get("errors").forEach(err -> errors.add(err.asText()));
                                    turnAnalysis.put("errors", errors);
                                }
                                if (node.has("corrections")) {
                                    List<String> corrections = new ArrayList<>();
                                    node.get("corrections").forEach(corr -> corrections.add(corr.asText()));
                                    turnAnalysis.put("corrections", corrections);
                                }
                                if (node.has("betterResponse")) {
                                    turnAnalysis.put("betterResponse", node.get("betterResponse").asText());
                                }
                                detailedAnalysis.add(turnAnalysis);
                            });
                            evaluation.put("detailedAnalysisVi", detailedAnalysis);
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
        evaluation.put("accuracyScore", 75.0);
        evaluation.put("fluencyScore", 75.0);
        evaluation.put("grammarScore", 75.0);
        evaluation.put("vocabularyScore", 75.0);
        evaluation.put("overallFeedbackVi", "Cuộc trò chuyện đã hoàn thành. Hãy tiếp tục luyện tập để cải thiện kỹ năng!");
        evaluation.put("strengthsVi", Arrays.asList("Đã hoàn thành cuộc trò chuyện", "Tham gia tích cực vào cuộc trò chuyện"));
        evaluation.put("improvementsVi", Arrays.asList("Tiếp tục luyện tập phát âm và từ vựng", "Chú ý hơn đến ngữ pháp và cách sử dụng trợ từ"));
        evaluation.put("suggestionsVi", Arrays.asList("Thử các tình huống khác nhau để mở rộng vốn từ", "Luyện tập thêm các mẫu câu thông dụng"));
        evaluation.put("detailedAnalysisVi", new ArrayList<>()); // Empty detailed analysis in fallback
        
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

