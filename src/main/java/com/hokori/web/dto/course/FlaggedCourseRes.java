package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho danh sách courses bị flag
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedCourseRes {
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private Long teacherId;
    private String teacherName;
    private Long flagCount; // Số lượng flags
    private Instant latestFlagAt; // Thời điểm flag gần nhất
    private String flaggedReason; // Lý do flag (tổng hợp)
    private Instant flaggedAt; // Thời điểm moderator flag
    private Long flaggedByUserId; // Moderator đã flag
    private String flaggedByUserName; // Tên moderator
    
    // Danh sách các flags chi tiết
    private List<FlagDetailRes> flags;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlagDetailRes {
        private Long flagId;
        private String flagType;
        private String reason;
        private Long userId;
        private String userName;
        private Instant createdAt;
    }
}

