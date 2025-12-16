package com.hokori.web.service;

import com.hokori.web.dto.dashboard.LearnerDashboardSummaryRes;
import com.hokori.web.entity.UserDailyLearning;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserDailyLearningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearnerDashboardService {

    private final EnrollmentRepository enrollmentRepo;
    private final LearnerProgressService learnerProgressService;
    private final UserDailyLearningRepository userDailyLearningRepo;

    public LearnerDashboardSummaryRes getSummary(Long userId) {
        long totalEnrolled = enrollmentRepo.countByUser_Id(userId);
        long totalCompleted = enrollmentRepo.countByUser_IdAndCompletedAtIsNotNull(userId);

        int streak = learnerProgressService.getCurrentLearningStreak(userId);

        var today = LocalDate.now(ZoneId.systemDefault());
        int todayActivity = userDailyLearningRepo.findByUserIdAndLearningDate(userId, today)
                .map(UserDailyLearning::getActivityCount)
                .orElse(0);

        return LearnerDashboardSummaryRes.builder()
                .currentLearningStreak(streak)
                .totalEnrolledCourses(totalEnrolled)
                .totalCompletedCourses(totalCompleted)
                .todayActivityCount(todayActivity)
                .build();
    }
}
