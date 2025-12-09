package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.ContentType;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRes {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private String description;
    private JLPTLevel level;
    private Long priceCents;
    private Long discountedPriceCents;
    private String currency;
    private String coverImagePath;
    private CourseStatus status;
    private Instant publishedAt;
    private Long userId;
    private String teacherName; // Tên giáo viên (displayName hoặc username)
    private List<ChapterRes> chapters;
    private Long enrollCount;
    private Boolean isEnrolled; // true nếu user hiện tại đã enroll vào course này (null nếu chưa đăng nhập)
    
    // Rejection info (chỉ có khi status = REJECTED)
    private String rejectionReason;
    private Instant rejectedAt;
    private Long rejectedByUserId;
    private String rejectedByUserName; // Tên moderator đã reject
    
    // Flag info (chỉ có khi status = FLAGGED)
    private String flaggedReason; // Lý do flag (tổng hợp từ các flags)
    private Instant flaggedAt;
    private Long flaggedByUserId;
    private String flaggedByUserName; // Tên moderator đã flag
    
    // Status message (thân thiện cho learner, không gây tiêu cực)
    private String statusMessage; // Message hiển thị cho learner khi status = FLAGGED
}
