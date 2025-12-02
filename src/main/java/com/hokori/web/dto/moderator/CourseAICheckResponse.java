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

