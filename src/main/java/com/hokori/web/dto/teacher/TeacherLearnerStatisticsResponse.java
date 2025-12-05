// com.hokori.web.dto.teacher.TeacherLearnerStatisticsResponse.java
package com.hokori.web.dto.teacher;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho teacher xem statistics của learners trong course.
 */
@Value
@Builder
public class TeacherLearnerStatisticsResponse {

    // Thông tin course
    Long courseId;
    String courseTitle;
    
    // Statistics tổng hợp
    CourseStatistics courseStatistics;
    
    // Danh sách learners với progress
    List<LearnerProgress> learners;

    @Value
    @Builder
    public static class CourseStatistics {
        int totalEnrollments;           // Tổng số learners đã enroll
        int activeLearners;             // Số learners đang học (có lastAccessAt gần đây)
        int completedLearners;          // Số learners đã hoàn thành (completedAt != null)
        double averageProgressPercent;  // % progress trung bình
        int minProgressPercent;         // % progress thấp nhất
        int maxProgressPercent;         // % progress cao nhất
    }

    @Value
    @Builder
    public static class LearnerProgress {
        Long learnerId;
        String learnerName;             // displayName hoặc username
        String learnerEmail;
        Integer progressPercent;         // % progress hiện tại
        Instant startedAt;               // Thời gian bắt đầu học
        Instant completedAt;             // Thời gian hoàn thành (null nếu chưa hoàn thành)
        Instant lastAccessAt;            // Lần truy cập cuối cùng
        Instant enrolledAt;              // Thời gian enroll (createdAt)
    }
}

