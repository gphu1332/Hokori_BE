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
}
