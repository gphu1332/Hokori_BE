package com.hokori.web.service;

import com.hokori.web.dto.teacher.TeacherLearnerStatisticsResponse;
import com.hokori.web.entity.Enrollment;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherStatisticsServiceTest {

    @Mock
    private CourseRepository courseRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CurrentUserService currentUser;

    @InjectMocks
    private TeacherStatisticsService teacherStatisticsService;

    /**
     * TC-TEACHER-STAT-01
     * Teacher là owner của course → trả về statistics
     */
    @Test
    void getCourseLearnerStatistics_owner_success() {
        Long courseId = 1L;
        Long teacherId = 10L;

        when(currentUser.getUserIdOrThrow()).thenReturn(teacherId);
        when(courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherId))
                .thenReturn(true);

        // course metadata: [id, title]
        when(courseRepo.findCourseMetadataById(courseId))
                .thenReturn(Optional.of(new Object[]{courseId, "JLPT N4 Course"}));

        Enrollment e1 = new Enrollment();
        e1.setUserId(100L);
        e1.setProgressPercent(50);
        e1.setCreatedAt(Instant.now());
        e1.setLastAccessAt(Instant.now());

        Enrollment e2 = new Enrollment();
        e2.setUserId(101L);
        e2.setProgressPercent(100);
        e2.setCompletedAt(Instant.now());
        e2.setCreatedAt(Instant.now());

        when(enrollmentRepo.findByCourse_IdOrderByCreatedAtDesc(courseId))
                .thenReturn(List.of(e1, e2));

        User learner1 = new User();
        learner1.setId(100L);
        learner1.setDisplayName("Learner A");
        learner1.setEmail("a@test.com");

        User learner2 = new User();
        learner2.setId(101L);
        learner2.setUsername("learner_b");
        learner2.setEmail("b@test.com");

        when(userRepo.findById(100L)).thenReturn(Optional.of(learner1));
        when(userRepo.findById(101L)).thenReturn(Optional.of(learner2));

        // when
        TeacherLearnerStatisticsResponse res =
                teacherStatisticsService.getCourseLearnerStatistics(courseId);

        // then
        assertNotNull(res);
        assertEquals(courseId, res.getCourseId());
        assertEquals("JLPT N4 Course", res.getCourseTitle());

        var stats = res.getCourseStatistics();
        assertEquals(2, stats.getTotalEnrollments());
        assertEquals(1, stats.getCompletedLearners());
        assertEquals(50, stats.getMinProgressPercent());
        assertEquals(100, stats.getMaxProgressPercent());

        assertEquals(2, res.getLearners().size());
    }

    /**
     * TC-TEACHER-STAT-02
     * Teacher không phải owner → FORBIDDEN
     */
    @Test
    void getCourseLearnerStatistics_notOwner_forbidden() {
        Long courseId = 2L;
        Long teacherId = 99L;

        when(currentUser.getUserIdOrThrow()).thenReturn(teacherId);
        when(courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherId))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> teacherStatisticsService.getCourseLearnerStatistics(courseId)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
