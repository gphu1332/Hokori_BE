package com.hokori.web.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerDashboardSummaryRes {

    /** Chuỗi ngày học liên tiếp (learning streak toàn hệ thống) */
    private int currentLearningStreak;

    /** Tổng số khoá học đã enroll */
    private long totalEnrolledCourses;

    /** Số khoá học đã hoàn thành (completedAt != null) */
    private long totalCompletedCourses;

    /** Số hoạt động học trong hôm nay (xem bài, quiz, JLPT, flashcard, ...) */
    private int todayActivityCount;
}
