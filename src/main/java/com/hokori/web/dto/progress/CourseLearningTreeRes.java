package com.hokori.web.dto.progress;

import com.hokori.web.Enum.CourseStatus;
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
    private String teacherName;
    private CourseStatus courseStatus;
    
    // Enrollment info
    private Long enrollmentId;
    private Integer progressPercent;
    private java.time.Instant lastAccessAt;
    
    // Tree structure with progress
    private List<ChapterLearningTreeRes> chapters;
}

