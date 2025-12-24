package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for course rejection with structured reasons.
 * FE có thể gửi reason cho từng phần riêng biệt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRejectionRequest {
    /**
     * General reason for the entire course (optional)
     */
    private String general;
    
    /**
     * Reason for course title (optional)
     */
    private String title;
    
    /**
     * Reason for course subtitle (optional)
     */
    private String subtitle;
    
    /**
     * Reason for course description (optional)
     */
    private String description;
    
    /**
     * Reason for cover image (optional)
     */
    private String coverImage;
    
    /**
     * Reason for pricing (optional)
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
     * Reasons for specific sections (quiz, grammar, flashcard vocab, etc.)
     * Mỗi section có thể có lý do rejection riêng.
     * Ví dụ: section quiz thiếu câu hỏi, section grammar sai ngữ pháp, section vocab thiếu từ vựng.
     */
    private List<ItemReason> sections = new ArrayList<>();
    
    /**
     * Inner class for item-specific reasons
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemReason {
        private Long id;
        private String reason;
    }
    
    /**
     * Check if request has any rejection reasons
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

