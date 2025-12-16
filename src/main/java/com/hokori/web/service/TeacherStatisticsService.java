// com.hokori.web.service.TeacherStatisticsService.java
package com.hokori.web.service;

import com.hokori.web.dto.teacher.TeacherLearnerStatisticsResponse;
import com.hokori.web.entity.Enrollment;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStatisticsService {

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final CurrentUserService currentUser;

    /**
     * Lấy statistics của learners trong một course.
     * Chỉ teacher owner của course mới được xem.
     */
    public TeacherLearnerStatisticsResponse getCourseLearnerStatistics(Long courseId) {
        Long teacherId = currentUser.getUserIdOrThrow();
        
        // Kiểm tra teacher là owner của course
        if (!courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner of this course");
        }
        
        // Lấy course metadata để lấy title (không load full entity với LOB)
        var courseMetadataOpt = courseRepo.findCourseMetadataById(courseId);
        if (courseMetadataOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        
        Object[] metadata = courseMetadataOpt.get();
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        String courseTitle = actualMetadata[1] != null ? actualMetadata[1].toString() : "Unknown";
        
        // Lấy tất cả enrollments của course này
        List<Enrollment> enrollments = enrollmentRepo.findByCourse_IdOrderByCreatedAtDesc(courseId);
        
        // Tính statistics tổng hợp
        int totalEnrollments = enrollments.size();
        int activeLearners = (int) enrollments.stream()
                .filter(e -> e.getLastAccessAt() != null && 
                        e.getLastAccessAt().isAfter(Instant.now().minusSeconds(7 * 24 * 60 * 60))) // 7 ngày gần đây
                .count();
        int completedLearners = (int) enrollments.stream()
                .filter(e -> e.getCompletedAt() != null)
                .count();
        
        // Tính % progress trung bình, min, max
        double averageProgress = enrollments.stream()
                .mapToInt(e -> e.getProgressPercent() != null ? e.getProgressPercent() : 0)
                .average()
                .orElse(0.0);
        
        int minProgress = enrollments.stream()
                .mapToInt(e -> e.getProgressPercent() != null ? e.getProgressPercent() : 0)
                .min()
                .orElse(0);
        
        int maxProgress = enrollments.stream()
                .mapToInt(e -> e.getProgressPercent() != null ? e.getProgressPercent() : 0)
                .max()
                .orElse(0);
        
        // Build course statistics
        TeacherLearnerStatisticsResponse.CourseStatistics courseStats = 
                TeacherLearnerStatisticsResponse.CourseStatistics.builder()
                        .totalEnrollments(totalEnrollments)
                        .activeLearners(activeLearners)
                        .completedLearners(completedLearners)
                        .averageProgressPercent(Math.round(averageProgress * 100.0) / 100.0) // Làm tròn 2 chữ số
                        .minProgressPercent(minProgress)
                        .maxProgressPercent(maxProgress)
                        .build();
        
        // Build learner progress list
        List<TeacherLearnerStatisticsResponse.LearnerProgress> learnerProgressList = enrollments.stream()
                .map(enrollment -> {
                    // Lấy thông tin user
                    User learner = userRepo.findById(enrollment.getUserId())
                            .orElse(null);
                    
                    String learnerName = learner != null ? 
                            (learner.getDisplayName() != null ? learner.getDisplayName() : learner.getUsername()) 
                            : "Unknown";
                    String learnerEmail = learner != null ? learner.getEmail() : null;
                    
                    return TeacherLearnerStatisticsResponse.LearnerProgress.builder()
                            .learnerId(enrollment.getUserId())
                            .learnerName(learnerName)
                            .learnerEmail(learnerEmail)
                            .progressPercent(enrollment.getProgressPercent() != null ? enrollment.getProgressPercent() : 0)
                            .startedAt(enrollment.getStartedAt())
                            .completedAt(enrollment.getCompletedAt())
                            .lastAccessAt(enrollment.getLastAccessAt())
                            .enrolledAt(enrollment.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        return TeacherLearnerStatisticsResponse.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .courseStatistics(courseStats)
                .learners(learnerProgressList)
                .build();
    }
}

