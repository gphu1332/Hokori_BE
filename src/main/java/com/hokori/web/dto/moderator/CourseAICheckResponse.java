package com.hokori.web.dto.moderator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for AI content moderation check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAICheckResponse {
    
    private Long courseId;
    private String courseTitle;
    private Instant checkedAt;
    
    private SafetyCheck safetyCheck;
    private LevelMatch levelMatch;
    
    // New AI checks with Gemini
    private PedagogicalQuality pedagogicalQuality;
    private LanguageAccuracy languageAccuracy;
    private GrammarProgression grammarProgression;
    
    private List<String> recommendations;
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyCheck {
        /**
         * Status: SAFE, WARNING, UNSAFE
         */
        private String status;
        
        /**
         * Safety score: 0.0 - 1.0 (1.0 = completely safe)
         */
        private Double score;
        
        private Boolean hasIssues;
        private String summary;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelMatch {
        /**
         * Declared level (from course.level)
         */
        private String declaredLevel;
        
        /**
         * Detected level by AI (optional, can be null if not checked)
         */
        private String detectedLevel;
        
        /**
         * Whether detected level matches declared level
         */
        private Boolean match;
        
        /**
         * Confidence score: 0.0 - 1.0
         */
        private Double confidence;
        
        private String summary;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedagogicalQuality {
        /**
         * Overall pedagogical quality score: 0.0 - 1.0
         */
        private Double score;
        
        /**
         * List of strengths identified
         */
        private List<String> strengths;
        
        /**
         * List of weaknesses identified
         */
        private List<String> weaknesses;
        
        /**
         * Summary of pedagogical quality assessment
         */
        private String summary;
        
        /**
         * Specific recommendations for improvement
         */
        private List<String> recommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageAccuracy {
        /**
         * Overall language accuracy score: 0.0 - 1.0
         */
        private Double score;
        
        /**
         * List of Japanese language errors found
         */
        private List<LanguageError> japaneseErrors;
        
        /**
         * List of Vietnamese translation errors found
         */
        private List<LanguageError> vietnameseErrors;
        
        /**
         * List of vocabulary/grammar level mismatches
         */
        private List<LevelMismatch> levelMismatches;
        
        /**
         * Summary of language accuracy assessment
         */
        private String summary;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageError {
        private String text;
        private String error;
        private String suggestion;
        private String location; // e.g., "Lesson 2, Section 1"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelMismatch {
        private String word;
        private String declaredLevel;
        private String actualLevel;
        private String location;
        private String severity; // HIGH, MEDIUM, LOW
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrammarProgression {
        /**
         * Whether grammar progression is logical
         */
        private Boolean isLogical;
        
        /**
         * Overall progression score: 0.0 - 1.0
         */
        private Double score;
        
        /**
         * List of progression issues found
         */
        private List<ProgressionIssue> issues;
        
        /**
         * Summary of grammar progression assessment
         */
        private String summary;
        
        /**
         * Recommendations for fixing progression issues
         */
        private List<String> recommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressionIssue {
        /**
         * Grammar point that has issues
         */
        private String grammar;
        
        /**
         * Current lesson where it appears
         */
        private String currentLocation;
        
        /**
         * Required prerequisite grammar
         */
        private String requiredPrerequisite;
        
        /**
         * Where prerequisite should be taught
         */
        private String prerequisiteLocation;
        
        /**
         * Severity: HIGH, MEDIUM, LOW
         */
        private String severity;
        
        /**
         * Description of the issue
         */
        private String description;
        
        /**
         * Potential confusion or misunderstanding this might cause
         */
        private String potentialConfusion;
    }
    
    public static CourseAICheckResponse createDefault(Long courseId, String courseTitle) {
        return CourseAICheckResponse.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .checkedAt(Instant.now())
                .recommendations(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }
}

