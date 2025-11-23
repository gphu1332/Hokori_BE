package com.hokori.web.repository;

import com.hokori.web.entity.CourseComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCommentRepository extends JpaRepository<CourseComment, Long> {

    // Root comment (parent null), chưa bị xóa
    Page<CourseComment> findByCourse_IdAndParentIsNullAndDeletedFlagFalseOrderByCreatedAtDesc(
            Long courseId,
            Pageable pageable
    );

    // Dùng khi cần lấy 1 comment (chưa bị xóa)
    Optional<CourseComment> findByIdAndDeletedFlagFalse(Long id);

    // Check cùng course
    Optional<CourseComment> findByIdAndCourse_IdAndDeletedFlagFalse(Long id, Long courseId);

    long countByCourse_IdAndDeletedFlagFalse(Long courseId);
}
