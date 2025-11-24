package com.hokori.web.dto.dashboard;

import com.hokori.web.Enum.CourseStatus;

import java.time.Instant;

public record RecentCourseSummaryDto(
        Long courseId,
        String title,
        String code,
        long students,
        Double rating,
        CourseStatus status,
        Instant updatedAt
) {}
