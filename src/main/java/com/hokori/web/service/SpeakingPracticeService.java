package com.hokori.web.service;

import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    // Scoring weights
    @Value("${kaiwa.scoring.accuracy-weight:0.6}")
    private double accuracyWeight;
    
    @Value("${kaiwa.scoring.pronunciation-weight:0.4}")
    private double pronunciationWeight;
    
    // Score adjustments by level
    @Value("${kaiwa.scoring.adjustment.n1:0.95}")
    private double adjustmentN1;
    
    @Value("${kaiwa.scoring.adjustment.n2:0.97}")
    private double adjustmentN2;
    
    @Value("${kaiwa.scoring.adjustment.n3:0.98}")
    private double adjustmentN3;
    
    @Value("${kaiwa.scoring.adjustment.n4:0.99}")
    private double adjustmentN4;
    
    @Value("${kaiwa.scoring.adjustment.n5:1.0}")
    private double adjustmentN5;
    
    // Accuracy thresholds by level
    @Value("${kaiwa.threshold.accuracy.n1:0.9}")
    private double accuracyThresholdN1;
    
    @Value("${kaiwa.threshold.accuracy.n2:0.85}")
    private double accuracyThresholdN2;
    
    @Value("${kaiwa.threshold.accuracy.n3:0.8}")
    private double accuracyThresholdN3;
    
    @Value("${kaiwa.threshold.accuracy.n4:0.75}")
    private double accuracyThresholdN4;
    
    @Value("${kaiwa.threshold.accuracy.n5:0.7}")
    private double accuracyThresholdN5;
    
    @Value("${kaiwa.threshold.accuracy.default:0.8}")
    private double accuracyThresholdDefault;
    
    // Practice thresholds by level
    @Value("${kaiwa.threshold.practice.n1:0.85}")
    private double practiceThresholdN1;
    
    @Value("${kaiwa.threshold.practice.n2:0.8}")
    private double practiceThresholdN2;
    
    @Value("${kaiwa.threshold.practice.n3:0.75}")
    private double practiceThresholdN3;
    
    @Value("${kaiwa.threshold.practice.n4:0.7}")
    private double practiceThresholdN4;
    
    @Value("${kaiwa.threshold.practice.n5:0.65}")
    private double practiceThresholdN5;
    
    @Value("${kaiwa.threshold.practice.default:0.7}")
    private double practiceThresholdDefault;
    
    // Tolerance by level
    @Value("${kaiwa.tolerance.n1:0.05}")
    private double toleranceN1;
    
    @Value("${kaiwa.tolerance.n2:0.08}")
    private double toleranceN2;
    
    @Value("${kaiwa.tolerance.n3:0.10}")
    private double toleranceN3;
    
    @Value("${kaiwa.tolerance.n4:0.12}")
    private double toleranceN4;
    
    @Value("${kaiwa.tolerance.n5:0.15}")
    private double toleranceN5;
    
    @Value("${kaiwa.tolerance.default:0.15}")
    private double toleranceDefault;
    
    // Partial match boost by level
    @Value("${kaiwa.partial-match-boost.n1:0.6}")
    private double partialMatchBoostN1;
    
    @Value("${kaiwa.partial-match-boost.n2:0.65}")
    private double partialMatchBoostN2;
    
    @Value("${kaiwa.partial-match-boost.n3:0.7}")
    private double partialMatchBoostN3;
    
    @Value("${kaiwa.partial-match-boost.n4:0.75}")
    private double partialMatchBoostN4;
    
    @Value("${kaiwa.partial-match-boost.n5:0.8}")
    private double partialMatchBoostN5;
    
    @Value("${kaiwa.partial-match-boost.default:0.7}")
    private double partialMatchBoostDefault;
    
    // Speaking rates by level
    @Value("${kaiwa.speaking-rate.n1:1.5}")
    private double speakingRateN1;
    
    @Value("${kaiwa.speaking-rate.n2:1.25}")
    private double speakingRateN2;
    
    @Value("${kaiwa.speaking-rate.n3:1.0}")
    private double speakingRateN3;
    
    @Value("${kaiwa.speaking-rate.n4:0.85}")
    private double speakingRateN4;
    
    @Value("${kaiwa.speaking-rate.n5:0.75}")
    private double speakingRateN5;
    
    @Value("${kaiwa.speaking-rate.default:1.0}")
    private double speakingRateDefault;
    
    // Feedback thresholds
    @Value("${ai.feedback.threshold.excellent:0.9}")
    private double feedbackThresholdExcellent;
    
    @Value("${ai.feedback.threshold.good:0.7}")
    private double feedbackThresholdGood;
    
    @Value("${ai.feedback.threshold.average:0.5}")
    private double feedbackThresholdAverage;
    
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
        return practiceKaiwa(targetText, audioData, language, level, null);
    }
    
    /**
     * Practice kaiwa (conversation) by comparing user's pronunciation with target text
     * 
     * <h3>Business Rules & Validation:</h3>
     * <ol>
     *   <li><b>Audio Validation:</b>
     *       <ul>
     *         <li>Base64 string length: Max 13MB (validated in DTO)</li>
     *         <li>Decoded file size: Max 10MB (Google Cloud Speech-to-Text long-running limit)</li>
     *         <li>Estimated duration: Max 60 seconds (Google Cloud synchronous API limit)</li>
     *         <li>Minimum length: 500 base64 characters (ensures valid audio)</li>
     *         <li>Note: Google Cloud synchronous API supports max 60s; audio > 60s will be rejected</li>
     *       </ul>
     *   </li>
     *   <li><b>Target Text:</b> Max 5000 characters (validated in DTO)</li>
     *   <li><b>Transcript Validation:</b>
     *       <ul>
     *         <li>After Speech-to-Text conversion, transcript length is validated</li>
     *         <li>Max transcript length: 5000 characters</li>
     *         <li>Typical transcript: 150-200 characters per minute of speech</li>
     *         <li>For 2 minutes: ~300-400 characters typical, max ~500 characters if speaking very fast</li>
     *       </ul>
     *   </li>
     *   <li><b>Error Handling:</b>
     *       <ul>
     *         <li>AUDIO_EMPTY: Audio data is null or empty</li>
     *         <li>AUDIO_TOO_SHORT: Base64 string < 500 characters</li>
     *         <li>INVALID_AUDIO_FORMAT: Invalid base64 encoding</li>
     *         <li>AUDIO_TOO_LARGE: Decoded file > 1MB</li>
     *         <li>AUDIO_TOO_LONG: Estimated duration > 60 seconds</li>
     *         <li>TRANSCRIPTION_FAILED: STT API failed to transcribe</li>
     *         <li>TRANSCRIPT_TOO_LONG: Transcript > 5000 characters</li>
     *       </ul>
     *   </li>
     * </ol>
     * 
     * @param targetText The Japanese text user should practice (max 5000 characters)
     * @param audioData Base64 encoded audio of user's pronunciation (max 13MB base64 = ~10MB decoded = ~2 minutes)
     * @param language Language code (default: "ja-JP")
     * @param level JLPT level: N5, N4, N3, N2, N1 (optional, default: N5)
     * @param audioFormat Audio format (e.g., "wav", "mp3", "ogg", "webm") - auto-detected if not provided
     * @return Map containing comparison results, scores, and feedback
     * @throws AIServiceException if validation fails or processing error occurs
     */
    public Map<String, Object> practiceKaiwa(String targetText, String audioData, String language, String level, String audioFormat) {
        // Normalize level (default to N5 if not provided)
        String normalizedLevel = normalizeLevel(level);
        
        logger.info("Starting kaiwa practice: targetTextLength={}, audioDataLength={}, language={}, level={}, audioFormat={}", 
            targetText != null ? targetText.length() : 0,
            audioData != null ? audioData.length() : 0,
            language,
            normalizedLevel,
            audioFormat);
        
        // Validate audio data before processing
        validateAudioData(audioData);
        
        // Step 1: Convert user's audio to text
        Map<String, Object> speechToTextResult = aiService.speechToText(audioData, language, audioFormat);
        String userTranscript = (String) speechToTextResult.get("transcript");
        Double confidence = null;
        Object confidenceObj = speechToTextResult.get("confidence");
        if (confidenceObj != null) {
            confidence = ((Number) confidenceObj).doubleValue();
        }
        
        if (userTranscript == null || userTranscript.isEmpty()) {
            throw new AIServiceException("Kaiwa Practice", 
                "Could not transcribe audio. Please try again with clearer pronunciation.", 
                "TRANSCRIPTION_FAILED");
        }
        
        // Validate transcript length (should match targetText max length)
        if (userTranscript.length() > 5000) {
            throw new AIServiceException("Kaiwa Practice",
                "Audio transcript is too long (max 5000 characters). Please record a shorter audio.",
                "TRANSCRIPT_TOO_LONG");
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
        double overallScore = (accuracyScore * accuracyWeight) + (pronunciationScore * pronunciationWeight);
        
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
                return score * adjustmentN1;
            case "N2":
                return score * adjustmentN2;
            case "N3":
                return score * adjustmentN3;
            case "N4":
                return score * adjustmentN4;
            case "N5":
            default:
                return score * adjustmentN5;
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
        double rate = getRecommendedSpeakingRate(normalizedLevel);
        
        if (rate >= 1.25) {
            return "fast";
        } else if (rate >= 0.85) {
            return "normal";
        } else {
            return "slow";
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
                return speakingRateN1;
            case "N2":
                return speakingRateN2;
            case "N3":
                return speakingRateN3;
            case "N4":
                return speakingRateN4;
            case "N5":
            default:
                return speakingRateN5;
        }
    }
    
    /**
     * Get accuracy threshold based on JLPT level
     * Higher levels require higher accuracy
     */
    private double getAccuracyThreshold(String level) {
        if (level == null) {
            return accuracyThresholdDefault;
        }
        
        switch (level) {
            case "N1":
                return accuracyThresholdN1;
            case "N2":
                return accuracyThresholdN2;
            case "N3":
                return accuracyThresholdN3;
            case "N4":
                return accuracyThresholdN4;
            case "N5":
            default:
                return accuracyThresholdN5;
        }
    }
    
    /**
     * Get practice threshold based on JLPT level
     * Higher levels have higher practice requirements
     */
    private double getPracticeThreshold(String level) {
        if (level == null) {
            return practiceThresholdDefault;
        }
        
        switch (level) {
            case "N1":
                return practiceThresholdN1;
            case "N2":
                return practiceThresholdN2;
            case "N3":
                return practiceThresholdN3;
            case "N4":
                return practiceThresholdN4;
            case "N5":
            default:
                return practiceThresholdN5;
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
            return toleranceDefault;
        }
        
        switch (level) {
            case "N1":
                return toleranceN1;
            case "N2":
                return toleranceN2;
            case "N3":
                return toleranceN3;
            case "N4":
                return toleranceN4;
            case "N5":
            default:
                return toleranceN5;
        }
    }
    
    /**
     * Get partial match boost score based on level
     * Lower levels get higher boost for partial matches (encouraging learning)
     */
    private double getPartialMatchBoost(String level) {
        if (level == null) {
            return partialMatchBoostDefault;
        }
        
        switch (level) {
            case "N1":
                return partialMatchBoostN1;
            case "N2":
                return partialMatchBoostN2;
            case "N3":
                return partialMatchBoostN3;
            case "N4":
                return partialMatchBoostN4;
            case "N5":
            default:
                return partialMatchBoostN5;
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
        
        if (overallScore >= feedbackThresholdExcellent) {
            return String.format("Xuất sắc! Bạn đã phát âm rất tốt ở trình độ %s (điểm tổng: %.0f%%). Tiếp tục phát huy!", 
                levelName, overallScore * 100);
        } else if (overallScore >= feedbackThresholdGood) {
            return String.format("Tốt! Phát âm của bạn khá ổn ở trình độ %s (điểm tổng: %.0f%%). Hãy luyện tập thêm để hoàn thiện hơn.", 
                levelName, overallScore * 100);
        } else if (overallScore >= feedbackThresholdAverage) {
            return String.format("Cần cải thiện ở trình độ %s (điểm tổng: %.0f%%). Hãy nghe lại và luyện tập nhiều hơn.", 
                levelName, overallScore * 100);
        } else {
            return String.format("Cần luyện tập nhiều hơn ở trình độ %s (điểm tổng: %.0f%%). Hãy nghe kỹ cách phát âm và thử lại.", 
                levelName, overallScore * 100);
        }
    }
    
    private String getAccuracyFeedbackVi(double accuracyScore, String target, String user, String level) {
        String levelName = getLevelNameVi(level);
        
        if (accuracyScore >= feedbackThresholdExcellent) {
            return String.format("Độ chính xác về từ vựng và ngữ pháp rất cao cho trình độ %s!", levelName);
        } else if (accuracyScore >= feedbackThresholdGood) {
            return String.format("Độ chính xác khá tốt cho trình độ %s, nhưng vẫn còn một số lỗi nhỏ.", levelName);
        } else if (accuracyScore >= feedbackThresholdAverage) {
            return String.format("Cần chú ý hơn về từ vựng và ngữ pháp ở trình độ %s.", levelName);
        } else {
            return String.format("Cần luyện tập nhiều hơn về từ vựng và ngữ pháp ở trình độ %s.", levelName);
        }
    }
    
    private String getPronunciationFeedbackVi(double pronunciationScore, Double confidence, String level) {
        String levelName = getLevelNameVi(level);
        
        if (pronunciationScore >= feedbackThresholdExcellent) {
            return String.format("Phát âm rất rõ ràng và tự nhiên ở trình độ %s (độ tin cậy: %.0f%%)!", 
                levelName, pronunciationScore * 100);
        } else if (pronunciationScore >= feedbackThresholdGood) {
            return String.format("Phát âm khá tốt ở trình độ %s (độ tin cậy: %.0f%%), nhưng cần luyện tập thêm.", 
                levelName, pronunciationScore * 100);
        } else if (pronunciationScore >= feedbackThresholdAverage) {
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
        
        if (accuracyScore < feedbackThresholdGood) {
            suggestions.append(String.format("• Hãy đọc kỹ câu mẫu trước khi phát âm (trình độ %s).\n", levelName));
            suggestions.append("• Chú ý đến từng từ và cách phát âm.\n");
        }
        
        if (pronunciationScore < feedbackThresholdGood) {
            suggestions.append("• Nghe lại audio mẫu nhiều lần.\n");
            suggestions.append("• Luyện tập phát âm từng từ một cách rõ ràng.\n");
        }
        
        if (overallScore < feedbackThresholdGood) {
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
    
    /**
     * Validate audio data before processing
     * 
     * <h3>Business Rules:</h3>
     * <ul>
     *   <li><b>Minimum:</b> Base64 string must be at least 500 characters (ensures valid audio content)</li>
     *   <li><b>Maximum Base64:</b> 13MB base64 string (validated in DTO)</li>
     *   <li><b>Maximum Decoded:</b> 10MB file size (Google Cloud Speech-to-Text long-running limit)</li>
     *   <li><b>Maximum Duration:</b> ~60 seconds (Google Cloud synchronous API limit)</li>
     *   <li><b>Note:</b> Synchronous API supports max 60s; audio > 60s may require long-running recognition</li>
     * </ul>
     * 
     * <h3>Validation Steps:</h3>
     * <ol>
     *   <li>Check audio data is not null/empty</li>
     *   <li>Check base64 string length ≥ 500 characters</li>
     *   <li>Decode base64 and validate encoding</li>
     *   <li>Check decoded file size ≤ 1MB</li>
     *   <li>Estimate duration (using ~20KB/second average) and check ≤ 60 seconds</li>
     * </ol>
     * 
     * <h3>Error Codes:</h3>
     * <ul>
     *   <li>AUDIO_EMPTY: Audio data is null or empty</li>
     *   <li>AUDIO_TOO_SHORT: Base64 string < 500 characters</li>
     *   <li>INVALID_AUDIO_FORMAT: Invalid base64 encoding</li>
     *   <li>AUDIO_TOO_LARGE: Decoded file > 10MB</li>
     *   <li>AUDIO_TOO_LONG: Estimated duration > 60 seconds</li>
     * </ul>
     * 
     * <h3>Note:</h3>
     * Base64 encoding increases size by ~33%, so 13MB base64 ≈ 10MB decoded.
     * Duration estimation uses conservative 20KB/second average (actual varies by format/bitrate).
     * Google Cloud synchronous API supports max 60s; audio > 60s will be rejected.
     * 
     * @param audioData Base64 encoded audio data (max 1.3MB base64 = ~1MB decoded = ~60 seconds)
     * @throws AIServiceException if validation fails with specific error code
     */
    private void validateAudioData(String audioData) {
        if (audioData == null || audioData.trim().isEmpty()) {
            throw new AIServiceException("Kaiwa Practice",
                "Audio data is required. Please record your pronunciation before submitting.",
                "AUDIO_EMPTY");
        }
        
        // Check minimum base64 length (at least 500 chars for valid audio)
        String trimmedAudio = audioData.trim();
        if (trimmedAudio.length() < 500) {
            throw new AIServiceException("Kaiwa Practice",
                "Audio recording is too short or empty. Please record your response (at least 1-2 seconds) before submitting.",
                "AUDIO_TOO_SHORT");
        }
        
        // Decode base64 and check file size
        byte[] audioBytes;
        try {
            audioBytes = java.util.Base64.getDecoder().decode(trimmedAudio);
        } catch (IllegalArgumentException e) {
            throw new AIServiceException("Kaiwa Practice",
                "Invalid audio data format. Please ensure the audio is properly encoded.",
                "INVALID_AUDIO_FORMAT");
        }
        
        // Check decoded file size (Google Cloud limit: 10MB for long-running, 1MB for synchronous)
        // For 2 minutes audio: ~2-3MB typical, max 10MB for long-running recognition
        final long MAX_AUDIO_SIZE_BYTES = 10485760L; // 10MB (for long-running recognition support)
        if (audioBytes.length > MAX_AUDIO_SIZE_BYTES) {
            double sizeMB = audioBytes.length / 1048576.0;
            throw new AIServiceException("Kaiwa Practice",
                String.format("Audio file is too large (%.2f MB). Maximum allowed size is 10 MB. Please record a shorter audio.", sizeMB),
                "AUDIO_TOO_LARGE");
        }
        
        // Estimate duration based on file size
        // Assumptions:
        // - WAV/WebM at 16kHz mono: ~32KB per second (16k samples/sec * 2 bytes/sample)
        // - MP3 at 128kbps: ~16KB per second
        // - Use conservative estimate: 20KB per second average
        final double BYTES_PER_SECOND_ESTIMATE = 20000.0; // 20KB per second
        final int MAX_DURATION_SECONDS = 60; // 60 seconds (Google Cloud synchronous API limit)
        
        double estimatedDurationSeconds = audioBytes.length / BYTES_PER_SECOND_ESTIMATE;
        
        if (estimatedDurationSeconds > MAX_DURATION_SECONDS) {
            throw new AIServiceException("Kaiwa Practice",
                String.format("Audio recording is too long (estimated %.1f seconds). Maximum allowed duration is %d seconds. Please record a shorter audio or split into multiple recordings.", 
                    estimatedDurationSeconds, MAX_DURATION_SECONDS),
                "AUDIO_TOO_LONG");
        }
        
        logger.debug("Audio validation passed: size={} bytes ({} MB), estimatedDuration={} seconds", 
            audioBytes.length, String.format("%.2f", audioBytes.length / 1048576.0), 
            String.format("%.1f", estimatedDurationSeconds));
    }
}

