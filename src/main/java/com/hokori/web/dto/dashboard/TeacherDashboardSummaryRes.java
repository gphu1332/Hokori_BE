package com.hokori.web.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TeacherDashboardSummaryRes(
        long activeStudents,
        long activeStudentsChangePercent, // % so với tháng trước (nếu chưa có dữ liệu thì cho 0)
        long publishedCourses,
        long draftsWaitingReview,
        BigDecimal monthlyRevenue,
        LocalDate nextPayoutDate,
        long newComments,  // để FE làm cái card “New comments” sau này
        List<RecentCourseSummaryDto> recentCourses
) {}
