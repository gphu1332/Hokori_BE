package com.hokori.web.dto.progress;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseLearningTreeRes {
    // Course info
    private Long courseId;
    private String courseTitle;
    private String courseSubtitle;
    private String coverImagePath;
    
    // Enrollment info
    private Long enrollmentId;
    private Integer progressPercent;
    private java.time.Instant lastAccessAt;
    
    // Tree structure with progress
    private List<ChapterLearningTreeRes> chapters;
}

