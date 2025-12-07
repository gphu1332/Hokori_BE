package com.hokori.web.repository;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);
    
    /**
     * Find the most recent enrollment for a user and course.
     * If user has multiple enrollments, returns the latest one.
     * Uses native query to ensure we get the most recent enrollment.
     */
    @Query(value = """
        SELECT * FROM enrollment
        WHERE user_id = :userId AND course_id = :courseId
        ORDER BY created_at DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<Enrollment> findLatestByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    List<Enrollment> findByUserId(Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // ====== NEW: đếm học viên active của teacher ======
    @Query("""
        select count(distinct e.userId)
        from Enrollment e
        where e.courseId in (
            select c.id
            from Course c
            where c.userId = :teacherId
              and c.status = :status
              and c.deletedFlag = false
        )
        """)
    long countActiveStudentsByTeacher(
            @Param("teacherId") Long teacherId,
            @Param("status") CourseStatus status
    );

    // ====== NEW: đếm enrollment theo course ======
// ✅ Đúng với entity có field courseId
    long countByCourseId(Long courseId);
    long countByUserId(Long userId);
    long countByUserIdAndCompletedAtIsNotNull(Long userId);
    
    // ====== Teacher: lấy danh sách learners trong course ======
    List<Enrollment> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    // ====== Admin: đếm tổng enrollments của teacher ======
    @Query("""
        select count(e)
        from Enrollment e
        where e.courseId in (
            select c.id
            from Course c
            where c.userId = :teacherId
              and c.deletedFlag = false
        )
        """)
    long countByCourse_UserId(@Param("teacherId") Long teacherId);
}
