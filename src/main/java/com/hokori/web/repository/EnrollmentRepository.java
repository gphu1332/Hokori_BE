package com.hokori.web.repository;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Use relationship paths: User_Id and Course_Id
    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    
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

    List<Enrollment> findByUser_Id(Long userId);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    // ====== NEW: đếm học viên active của teacher ======
    // Đếm tất cả học viên từ các courses PUBLISHED và FLAGGED của teacher
    // (FLAGGED courses vẫn có học viên đã enroll, nên vẫn tính vào tổng số)
    @Query("""
        select count(distinct e.user.id)
        from Enrollment e
        where e.course.id in (
            select c.id
            from Course c
            where c.userId = :teacherId
              and c.status in :statuses
              and c.deletedFlag = false
        )
        """)
    long countActiveStudentsByTeacher(
            @Param("teacherId") Long teacherId,
            @Param("statuses") List<CourseStatus> statuses
    );

    // ====== NEW: đếm enrollment theo course ======
    // Use relationship paths: Course_Id and User_Id
    long countByCourse_Id(Long courseId);
    long countByUser_Id(Long userId);
    long countByUser_IdAndCompletedAtIsNotNull(Long userId);
    
    // ====== Teacher: lấy danh sách learners trong course ======
    List<Enrollment> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    // ====== Admin: đếm tổng enrollments của teacher ======
    @Query("""
        select count(e)
        from Enrollment e
        where e.course.id in (
            select c.id
            from Course c
            where c.userId = :teacherId
              and c.deletedFlag = false
        )
        """)
    long countByCourse_UserId(@Param("teacherId") Long teacherId);
}
