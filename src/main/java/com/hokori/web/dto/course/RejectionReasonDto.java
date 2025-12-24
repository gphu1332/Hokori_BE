package com.hokori.web.dto.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for structured rejection reasons returned to FE.
 * Built from separate database fields (not parsed from JSON string).
 * 
 * This DTO is populated from:
 * - Course fields: rejection_reason_general, rejection_reason_title, etc.
 * - CourseRejectionReasonDetail table: for chapters, lessons, sections
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RejectionReasonDto {
    /**
     * General reason for the entire course
     */
    private String general;
    
    /**
     * Reason for course title
     */
    private String title;
    
    /**
     * Reason for course subtitle
     */
    private String subtitle;
    
    /**
     * Reason for course description
     */
    private String description;
    
    /**
     * Reason for cover image
     */
    private String coverImage;
    
    /**
     * Reason for pricing
     */
    private String price;
    
    /**
     * Reasons for specific chapters
     */
    private List<ItemReason> chapters = new ArrayList<>();
    
    /**
     * Reasons for specific lessons
     */
    private List<ItemReason> lessons = new ArrayList<>();
    
    /**
     * Reasons for specific sections
     */
    private List<ItemReason> sections = new ArrayList<>();
    
    /**
     * Inner class for item-specific reasons
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemReason {
        private Long id;
        private String reason;
    }
    
    
    /**
     * Check if this DTO has any rejection reason
     */
    public boolean hasAnyReason() {
        return (general != null && !general.trim().isEmpty())
            || (title != null && !title.trim().isEmpty())
            || (subtitle != null && !subtitle.trim().isEmpty())
            || (description != null && !description.trim().isEmpty())
            || (coverImage != null && !coverImage.trim().isEmpty())
            || (price != null && !price.trim().isEmpty())
            || (chapters != null && !chapters.isEmpty())
            || (lessons != null && !lessons.isEmpty())
            || (sections != null && !sections.isEmpty());
    }
}

