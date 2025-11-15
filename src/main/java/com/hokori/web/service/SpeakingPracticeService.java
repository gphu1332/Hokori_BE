package com.hokori.web.service;

import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Speaking Practice (Kaiwa) feature
 * Handles pronunciation comparison, scoring, and feedback generation
 */
@Service
public class SpeakingPracticeService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpeakingPracticeService.class);
    
    @Autowired
    private AIService aiService;
    
    @Autowired(required = false)
    private AIResponseFormatter responseFormatter;
    
    /**
     * Practice kaiwa (conversation) by comparing user's pronunciation with target text
     * 
     * @param targetText The Japanese text user should practice
     * @param audioData Base64 encoded audio of user's pronunciation
     * @param language Language code (default: "ja-JP")
     * @param level JLPT level: N5, N4, N3, N2, N1 (optional, default: N5)
     * @return Map containing comparison results, scores, and feedback
     */
    public Map<String, Object> practiceKaiwa(String targetText, String audioData, String language, String level) {
        // Normalize level (default to N5 if not provided)
        String normalizedLevel = normalizeLevel(level);
        
        logger.info("Starting kaiwa practice: targetTextLength={}, audioDataLength={}, language={}, level={}", 
            targetText != null ? targetText.length() : 0,
            audioData != null ? audioData.length() : 0,
            language,
            normalizedLevel);
        
        // Step 1: Convert user's audio to text
        Map<String, Object> speechToTextResult = aiService.speechToText(audioData, language);
        String userTranscript = (String) speechToTextResult.get("transcript");
        Double confidence = (Double) speechToTextResult.get("confidence");
        
        if (userTranscript == null || userTranscript.isEmpty()) {
            throw new AIServiceException("Kaiwa Practice", 
                "Could not transcribe audio. Please try again with clearer pronunciation.", 
                "TRANSCRIPTION_FAILED");
        }
        
        logger.debug("User transcript: {}, confidence: {}", userTranscript, confidence);
        
        // Step 2: Normalize texts for comparison
        String normalizedTarget = normalizeJapaneseText(targetText);
        String normalizedUser = normalizeJapaneseText(userTranscript);
        
        // Step 3: Calculate accuracy score (with level-adjusted tolerance)
        double accuracyScore = calculateAccuracy(normalizedTarget, normalizedUser, normalizedLevel);
        
        // Step 4: Calculate pronunciation score (based on confidence)
        double pronunciationScore = confidence != null ? confidence : 0.0;
        
        // Step 5: Calculate overall score (weighted average)
        double overallScore = (accuracyScore * 0.6) + (pronunciationScore * 0.4);
        
        // Step 6: Adjust scoring based on JLPT level (higher levels = stricter scoring)
        double adjustedOverallScore = adjustScoreByLevel(overallScore, normalizedLevel);
        double adjustedAccuracyScore = adjustScoreByLevel(accuracyScore, normalizedLevel);
        
        // Step 7: Generate detailed feedback
        Map<String, Object> feedback = generateFeedback(
            targetText, 
            userTranscript, 
            adjustedAccuracyScore, 
            pronunciationScore, 
            adjustedOverallScore,
            confidence,
            normalizedLevel
        );
        
        // Step 8: Build result
        Map<String, Object> result = new HashMap<>();
        result.put("targetText", targetText);
        result.put("userTranscript", userTranscript);
        result.put("level", normalizedLevel);
        result.put("accuracyScore", adjustedAccuracyScore);
        result.put("pronunciationScore", pronunciationScore);
        result.put("overallScore", adjustedOverallScore);
        result.put("confidence", confidence);
        result.put("feedback", feedback);
        
        // Adjust thresholds based on level (higher levels = stricter)
        double accuracyThreshold = getAccuracyThreshold(normalizedLevel);
        double practiceThreshold = getPracticeThreshold(normalizedLevel);
        
        result.put("isAccurate", adjustedAccuracyScore >= accuracyThreshold);
        result.put("needsPractice", adjustedOverallScore < practiceThreshold);
        
        // Add level-specific recommendations
        Map<String, Object> recommendations = new HashMap<>();
        recommendations.put("recommendedSpeed", getRecommendedSpeed(normalizedLevel));
        recommendations.put("recommendedSpeakingRate", getRecommendedSpeakingRate(normalizedLevel));
        recommendations.put("tolerance", getToleranceByLevel(normalizedLevel));
        recommendations.put("accuracyThreshold", accuracyThreshold);
        recommendations.put("practiceThreshold", practiceThreshold);
        result.put("recommendations", recommendations);
        
        // Format response for Vietnamese users
        if (responseFormatter != null) {
            result = responseFormatter.formatKaiwaPracticeResponse(result);
        }
        
        logger.info("Kaiwa practice completed: level={}, overallScore={}, accuracyScore={}, pronunciationScore={}", 
            normalizedLevel, adjustedOverallScore, adjustedAccuracyScore, pronunciationScore);
        
        return result;
    }
    
    /**
     * Normalize JLPT level (default to N5 if not provided or invalid)
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
        return "N5"; // Default to N5 if invalid
    }
    
    /**
     * Adjust score based on JLPT level
     * Higher levels (N1, N2) have stricter scoring expectations
     * This reflects that advanced learners should be held to higher standards
     */
    private double adjustScoreByLevel(double score, String level) {
        if (level == null) {
            return score;
        }
        
        // For higher levels, slightly reduce score to reflect higher expectations
        // This encourages advanced learners to strive for perfection
        switch (level) {
            case "N1":
                return score * 0.95; // Stricter for N1 - expects near-perfect pronunciation
            case "N2":
                return score * 0.97; // Slightly stricter for N2
            case "N3":
                return score * 0.98; // Slightly stricter for N3
            case "N4":
                return score * 0.99; // Very slight adjustment for N4
            case "N5":
            default:
                return score; // No adjustment for beginner levels - encourage learning
        }
    }
    
    /**
     * Get recommended audio speed for Text-to-Speech based on JLPT level
     * Lower levels get slower speeds to help learners understand better
     * 
     * @param level JLPT level
     * @return Recommended speed: "slow", "normal", or "fast"
     */
    public String getRecommendedSpeed(String level) {
        String normalizedLevel = normalizeLevel(level);
        
        switch (normalizedLevel) {
            case "N1":
                return "fast"; // Advanced learners should practice at natural speed
            case "N2":
                return "normal"; // Upper-intermediate: normal speed
            case "N3":
                return "normal"; // Intermediate: normal speed
            case "N4":
                return "slow"; // Lower-intermediate: slower for clarity
            case "N5":
            default:
                return "slow"; // Beginners: slowest for learning
        }
    }
    
    /**
     * Get recommended speaking rate (0.25 to 4.0) for Text-to-Speech based on level
     * 
     * @param level JLPT level
     * @return Speaking rate value
     */
    public double getRecommendedSpeakingRate(String level) {
        String normalizedLevel = normalizeLevel(level);
        
        switch (normalizedLevel) {
            case "N1":
                return 1.5; // Fast - natural conversation speed
            case "N2":
                return 1.25; // Slightly faster than normal
            case "N3":
                return 1.0; // Normal speed
            case "N4":
                return 0.85; // Slightly slower
            case "N5":
            default:
                return 0.75; // Slow - helps beginners learn
        }
    }
    
    /**
     * Get accuracy threshold based on JLPT level
     * Higher levels require higher accuracy
     */
    private double getAccuracyThreshold(String level) {
        if (level == null) {
            return 0.8;
        }
        
        switch (level) {
            case "N1":
                return 0.9; // N1 requires 90% accuracy
            case "N2":
                return 0.85; // N2 requires 85% accuracy
            case "N3":
                return 0.8; // N3 requires 80% accuracy
            case "N4":
                return 0.75; // N4 requires 75% accuracy
            case "N5":
            default:
                return 0.7; // N5 requires 70% accuracy
        }
    }
    
    /**
     * Get practice threshold based on JLPT level
     * Higher levels have higher practice requirements
     */
    private double getPracticeThreshold(String level) {
        if (level == null) {
            return 0.7;
        }
        
        switch (level) {
            case "N1":
                return 0.85; // N1 needs practice if < 85%
            case "N2":
                return 0.8; // N2 needs practice if < 80%
            case "N3":
                return 0.75; // N3 needs practice if < 75%
            case "N4":
                return 0.7; // N4 needs practice if < 70%
            case "N5":
            default:
                return 0.65; // N5 needs practice if < 65%
        }
    }
    
    /**
     * Normalize Japanese text for comparison
     * Removes punctuation, converts to hiragana/katakana, removes spaces
     */
    private String normalizeJapaneseText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Remove punctuation and special characters
        String normalized = text.replaceAll("[。、！？・「」『』（）【】［］｛｝〈〉《》]", "");
        
        // Remove spaces
        normalized = normalized.replaceAll("\\s+", "");
        
        // Normalize Unicode (e.g., full-width to half-width)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        
        // Convert to lowercase for consistency (though Japanese doesn't have case)
        normalized = normalized.toLowerCase();
        
        return normalized.trim();
    }
    
    /**
     * Calculate accuracy score by comparing target and user text
     * Uses character-level comparison with tolerance adjusted by JLPT level
     * Lower levels get more tolerance for minor errors
     */
    private double calculateAccuracy(String target, String user, String level) {
        if (target == null || target.isEmpty()) {
            return 0.0;
        }
        
        if (user == null || user.isEmpty()) {
            return 0.0;
        }
        
        // Exact match
        if (target.equals(user)) {
            return 1.0;
        }
        
        // Calculate Levenshtein distance for similarity
        int distance = levenshteinDistance(target, user);
        int maxLength = Math.max(target.length(), user.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        // Similarity score: 1 - (distance / maxLength)
        double similarity = 1.0 - ((double) distance / maxLength);
        
        // Adjust tolerance based on level
        // Lower levels (N5, N4) get more tolerance for learning
        // Higher levels (N1, N2) require more precision
        double tolerance = getToleranceByLevel(level);
        
        // Also check if user text contains target text (partial match)
        if (user.contains(target) || target.contains(user)) {
            // Boost score for partial match, adjusted by level
            double partialMatchBoost = getPartialMatchBoost(level);
            similarity = Math.max(similarity, partialMatchBoost);
        }
        
        // Apply tolerance adjustment
        // For lower levels, small errors are more acceptable
        if (similarity >= (1.0 - tolerance)) {
            // If similarity is within tolerance, boost score slightly
            similarity = Math.min(1.0, similarity + (tolerance * 0.1));
        }
        
        return Math.max(0.0, Math.min(1.0, similarity));
    }
    
    /**
     * Get tolerance level for accuracy calculation based on JLPT level
     * Lower levels have higher tolerance (more forgiving)
     */
    private double getToleranceByLevel(String level) {
        if (level == null) {
            return 0.15; // Default tolerance for N5
        }
        
        switch (level) {
            case "N1":
                return 0.05; // Very strict - only 5% tolerance
            case "N2":
                return 0.08; // Strict - 8% tolerance
            case "N3":
                return 0.10; // Moderate - 10% tolerance
            case "N4":
                return 0.12; // More lenient - 12% tolerance
            case "N5":
            default:
                return 0.15; // Most lenient - 15% tolerance for beginners
        }
    }
    
    /**
     * Get partial match boost score based on level
     * Lower levels get higher boost for partial matches (encouraging learning)
     */
    private double getPartialMatchBoost(String level) {
        if (level == null) {
            return 0.7; // Default for N5
        }
        
        switch (level) {
            case "N1":
                return 0.6; // Lower boost - expects more precision
            case "N2":
                return 0.65; // Moderate boost
            case "N3":
                return 0.7; // Standard boost
            case "N4":
                return 0.75; // Higher boost - encourages learning
            case "N5":
            default:
                return 0.8; // Highest boost - very encouraging for beginners
        }
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Generate detailed feedback in Vietnamese
     */
    private Map<String, Object> generateFeedback(
        String targetText,
        String userTranscript,
        double accuracyScore,
        double pronunciationScore,
        double overallScore,
        Double confidence,
        String level
    ) {
        Map<String, Object> feedback = new HashMap<>();
        
        // Overall feedback
        String overallFeedbackVi = getOverallFeedbackVi(overallScore, level);
        feedback.put("overallFeedbackVi", overallFeedbackVi);
        
        // Accuracy feedback
        String accuracyFeedbackVi = getAccuracyFeedbackVi(accuracyScore, targetText, userTranscript, level);
        feedback.put("accuracyFeedbackVi", accuracyFeedbackVi);
        
        // Pronunciation feedback
        String pronunciationFeedbackVi = getPronunciationFeedbackVi(pronunciationScore, confidence, level);
        feedback.put("pronunciationFeedbackVi", pronunciationFeedbackVi);
        
        // Suggestions
        String suggestionsVi = getSuggestionsVi(overallScore, accuracyScore, pronunciationScore, level);
        feedback.put("suggestionsVi", suggestionsVi);
        
        // Pronunciation tips based on common errors
        List<String> pronunciationTips = detectPronunciationIssues(targetText, userTranscript, level);
        feedback.put("pronunciationTips", pronunciationTips);
        
        // Level-specific information
        Map<String, Object> levelInfo = new HashMap<>();
        levelInfo.put("level", level);
        levelInfo.put("levelNameVi", getLevelNameVi(level));
        levelInfo.put("descriptionVi", getLevelDescriptionVi(level));
        feedback.put("levelInfo", levelInfo);
        
        // Detailed comparison
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("targetText", targetText);
        comparison.put("userTranscript", userTranscript);
        comparison.put("match", targetText.equals(userTranscript));
        comparison.put("similarity", accuracyScore);
        
        // Character-level differences
        List<Map<String, Object>> differences = findCharacterDifferences(targetText, userTranscript);
        comparison.put("differences", differences);
        feedback.put("comparison", comparison);
        
        return feedback;
    }
    
    /**
     * Detect common pronunciation issues for Vietnamese learners
     */
    private List<String> detectPronunciationIssues(String target, String user, String level) {
        List<String> tips = new ArrayList<>();
        
        if (target == null || user == null || target.equals(user)) {
            return tips;
        }
        
        // Common issues for Vietnamese learners
        // 1. Long vowels (長音)
        if (target.contains("ー") && !user.contains("ー")) {
            tips.add("Chú ý: Trong tiếng Nhật có âm dài (長音). Hãy kéo dài nguyên âm khi phát âm.");
        }
        
        // 2. Small っ (促音)
        if (target.contains("っ") && !user.contains("っ")) {
            tips.add("Chú ý: Ký tự nhỏ っ (tsu) tạo ra âm ngắt. Hãy dừng một chút trước khi phát âm âm tiếp theo.");
        }
        
        // 3. Small ゃ, ゅ, ょ (拗音)
        if ((target.contains("ゃ") || target.contains("ゅ") || target.contains("ょ")) &&
            !(user.contains("ゃ") || user.contains("ゅ") || user.contains("ょ"))) {
            tips.add("Chú ý: Các ký tự nhỏ ゃ, ゅ, ょ tạo ra âm ghép. Hãy phát âm chúng cùng với phụ âm đứng trước.");
        }
        
        // 4. Particle は vs わ
        if (target.contains("は") && user.contains("わ") && !target.contains("わ")) {
            tips.add("Chú ý: Trợ từ は được phát âm là 'wa', không phải 'ha'. Nhưng khi viết vẫn dùng は.");
        }
        
        // 5. Particle を vs お
        if (target.contains("を") && user.contains("お") && !target.contains("お")) {
            tips.add("Chú ý: Trợ từ を được phát âm là 'o', nhưng khi viết vẫn dùng を.");
        }
        
        // 6. Length difference (common issue)
        int lengthDiff = Math.abs(target.length() - user.length());
        if (lengthDiff > 2) {
            tips.add("Chú ý: Độ dài câu của bạn khác với câu mẫu. Hãy nghe kỹ và đếm số lượng từ.");
        }
        
        // 7. Missing particles (for N3+)
        if (!level.equals("N5") && !level.equals("N4")) {
            if (target.contains("が") && !user.contains("が")) {
                tips.add("Chú ý: Bạn có thể đã bỏ sót trợ từ が. Hãy chú ý đến các trợ từ trong câu.");
            }
            if (target.contains("に") && !user.contains("に")) {
                tips.add("Chú ý: Bạn có thể đã bỏ sót trợ từ に. Hãy chú ý đến các trợ từ trong câu.");
            }
        }
        
        return tips;
    }
    
    /**
     * Find character-level differences between target and user text
     */
    private List<Map<String, Object>> findCharacterDifferences(String target, String user) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        if (target == null || user == null) {
            return differences;
        }
        
        int maxLength = Math.max(target.length(), user.length());
        for (int i = 0; i < maxLength; i++) {
            char targetChar = i < target.length() ? target.charAt(i) : ' ';
            char userChar = i < user.length() ? user.charAt(i) : ' ';
            
            if (targetChar != userChar) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("position", i);
                diff.put("expected", String.valueOf(targetChar));
                diff.put("actual", String.valueOf(userChar));
                diff.put("type", getDifferenceType(targetChar, userChar));
                differences.add(diff);
            }
        }
        
        return differences;
    }
    
    /**
     * Classify difference type
     */
    private String getDifferenceType(char expected, char actual) {
        // Check if it's a particle issue
        if ((expected == 'は' && actual == 'わ') || (expected == 'を' && actual == 'お')) {
            return "particle_pronunciation";
        }
        
        // Check if it's a long vowel issue
        if (expected == 'ー' || actual == 'ー') {
            return "long_vowel";
        }
        
        // Check if it's a small character issue
        if (expected == 'っ' || actual == 'っ' || 
            expected == 'ゃ' || actual == 'ゃ' ||
            expected == 'ゅ' || actual == 'ゅ' ||
            expected == 'ょ' || actual == 'ょ') {
            return "small_character";
        }
        
        // Check if it's a missing character
        if (expected != ' ' && actual == ' ') {
            return "missing_character";
        }
        
        // Check if it's an extra character
        if (expected == ' ' && actual != ' ') {
            return "extra_character";
        }
        
        return "character_mismatch";
    }
    
    private String getOverallFeedbackVi(double overallScore, String level) {
        String levelName = getLevelNameVi(level);
        
        if (overallScore >= 0.9) {
            return String.format("Xuất sắc! Bạn đã phát âm rất tốt ở trình độ %s (điểm tổng: %.0f%%). Tiếp tục phát huy!", 
                levelName, overallScore * 100);
        } else if (overallScore >= 0.7) {
            return String.format("Tốt! Phát âm của bạn khá ổn ở trình độ %s (điểm tổng: %.0f%%). Hãy luyện tập thêm để hoàn thiện hơn.", 
                levelName, overallScore * 100);
        } else if (overallScore >= 0.5) {
            return String.format("Cần cải thiện ở trình độ %s (điểm tổng: %.0f%%). Hãy nghe lại và luyện tập nhiều hơn.", 
                levelName, overallScore * 100);
        } else {
            return String.format("Cần luyện tập nhiều hơn ở trình độ %s (điểm tổng: %.0f%%). Hãy nghe kỹ cách phát âm và thử lại.", 
                levelName, overallScore * 100);
        }
    }
    
    private String getAccuracyFeedbackVi(double accuracyScore, String target, String user, String level) {
        String levelName = getLevelNameVi(level);
        
        if (accuracyScore >= 0.9) {
            return String.format("Độ chính xác về từ vựng và ngữ pháp rất cao cho trình độ %s!", levelName);
        } else if (accuracyScore >= 0.7) {
            return String.format("Độ chính xác khá tốt cho trình độ %s, nhưng vẫn còn một số lỗi nhỏ.", levelName);
        } else if (accuracyScore >= 0.5) {
            return String.format("Cần chú ý hơn về từ vựng và ngữ pháp ở trình độ %s.", levelName);
        } else {
            return String.format("Cần luyện tập nhiều hơn về từ vựng và ngữ pháp ở trình độ %s.", levelName);
        }
    }
    
    private String getPronunciationFeedbackVi(double pronunciationScore, Double confidence, String level) {
        String levelName = getLevelNameVi(level);
        
        if (pronunciationScore >= 0.9) {
            return String.format("Phát âm rất rõ ràng và tự nhiên ở trình độ %s (độ tin cậy: %.0f%%)!", 
                levelName, pronunciationScore * 100);
        } else if (pronunciationScore >= 0.7) {
            return String.format("Phát âm khá tốt ở trình độ %s (độ tin cậy: %.0f%%), nhưng cần luyện tập thêm.", 
                levelName, pronunciationScore * 100);
        } else if (pronunciationScore >= 0.5) {
            return String.format("Phát âm cần cải thiện ở trình độ %s (độ tin cậy: %.0f%%). Hãy nghe kỹ và luyện tập.", 
                levelName, pronunciationScore * 100);
        } else {
            return String.format("Phát âm cần luyện tập nhiều hơn ở trình độ %s (độ tin cậy: %.0f%%). Hãy nghe lại và thử lại.", 
                levelName, pronunciationScore * 100);
        }
    }
    
    private String getSuggestionsVi(double overallScore, double accuracyScore, double pronunciationScore, String level) {
        StringBuilder suggestions = new StringBuilder();
        String levelName = getLevelNameVi(level);
        
        if (accuracyScore < 0.7) {
            suggestions.append(String.format("• Hãy đọc kỹ câu mẫu trước khi phát âm (trình độ %s).\n", levelName));
            suggestions.append("• Chú ý đến từng từ và cách phát âm.\n");
        }
        
        if (pronunciationScore < 0.7) {
            suggestions.append("• Nghe lại audio mẫu nhiều lần.\n");
            suggestions.append("• Luyện tập phát âm từng từ một cách rõ ràng.\n");
        }
        
        if (overallScore < 0.7) {
            suggestions.append(String.format("• Luyện tập thường xuyên để đạt chuẩn trình độ %s.\n", levelName));
            suggestions.append("• Ghi âm và so sánh với audio mẫu.\n");
        }
        
        if (suggestions.length() == 0) {
            suggestions.append(String.format("• Tiếp tục luyện tập để duy trì kỹ năng ở trình độ %s.\n", levelName));
            if (!level.equals("N1")) {
                suggestions.append("• Thử các câu khó hơn hoặc nâng cấp lên trình độ cao hơn.");
            } else {
                suggestions.append("• Thử các câu phức tạp hơn để nâng cao trình độ.");
            }
        }
        
        return suggestions.toString();
    }
    
    private String getLevelNameVi(String level) {
        if (level == null) {
            return "N5";
        }
        switch (level) {
            case "N1":
                return "N1 (Cao cấp)";
            case "N2":
                return "N2 (Trung-Cao cấp)";
            case "N3":
                return "N3 (Trung cấp)";
            case "N4":
                return "N4 (Sơ-Trung cấp)";
            case "N5":
            default:
                return "N5 (Sơ cấp)";
        }
    }
    
    private String getLevelDescriptionVi(String level) {
        if (level == null) {
            return "Trình độ sơ cấp";
        }
        switch (level) {
            case "N1":
                return "Trình độ cao cấp - Có thể hiểu tiếng Nhật trong các tình huống đa dạng";
            case "N2":
                return "Trình độ trung-cao cấp - Có thể hiểu tiếng Nhật trong các tình huống hàng ngày và một phần trong các tình huống đa dạng";
            case "N3":
                return "Trình độ trung cấp - Có thể hiểu tiếng Nhật trong các tình huống hàng ngày ở một mức độ nhất định";
            case "N4":
                return "Trình độ sơ-trung cấp - Có thể hiểu tiếng Nhật cơ bản";
            case "N5":
            default:
                return "Trình độ sơ cấp - Có thể hiểu một phần tiếng Nhật cơ bản";
        }
    }
}

