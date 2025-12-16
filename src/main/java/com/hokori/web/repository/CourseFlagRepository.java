package com.hokori.web.repository;

import com.hokori.web.entity.CourseFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseFlagRepository extends JpaRepository<CourseFlag, Long> {

    /**
     * Đếm số lượng flag của một course
     */
    long countByCourse_Id(Long courseId);

    /**
     * Kiểm tra user đã flag course này chưa
     */
    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);

    /**
     * Lấy flag của user cho course này (nếu có)
     */
    Optional<CourseFlag> findByCourse_IdAndUser_Id(Long courseId, Long userId);

    /**
     * Lấy tất cả flags của một course
     */
    List<CourseFlag> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    /**
     * Lấy danh sách courses bị flag với số lượng flag (sắp xếp từ cao đến thấp)
     * Chỉ lấy courses có status = PUBLISHED hoặc FLAGGED
     */
    @Query(value = """
        SELECT cf.course_id, COUNT(cf.id) as flag_count
        FROM course_flag cf
        INNER JOIN course c ON c.id = cf.course_id
        WHERE c.status IN ('PUBLISHED', 'FLAGGED')
          AND c.deleted_flag = false
        GROUP BY cf.course_id
        ORDER BY flag_count DESC
    """, nativeQuery = true)
    List<Object[]> findFlaggedCoursesWithCount();

    /**
     * Lấy danh sách courses bị flag với thông tin chi tiết
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.user_id, c.status, 
               COUNT(cf.id) as flag_count,
               MAX(cf.created_at) as latest_flag_at
        FROM course_flag cf
        INNER JOIN course c ON c.id = cf.course_id
        WHERE c.status IN ('PUBLISHED', 'FLAGGED')
          AND c.deleted_flag = false
        GROUP BY c.id, c.title, c.slug, c.user_id, c.status
        ORDER BY flag_count DESC, latest_flag_at DESC
    """, nativeQuery = true)
    List<Object[]> findFlaggedCoursesWithDetails();
}

