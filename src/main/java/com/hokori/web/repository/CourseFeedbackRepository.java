// com.hokori.web.repository.CourseFeedbackRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.CourseFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseFeedbackRepository extends JpaRepository<CourseFeedback, Long> {

    List<CourseFeedback> findByCourse_IdAndDeletedFlagFalseOrderByCreatedAtDesc(Long courseId);

    Optional<CourseFeedback> findByCourse_IdAndUser_IdAndDeletedFlagFalse(Long courseId, Long userId);

    @Query("""
           SELECT COALESCE(AVG(f.rating), 0), COUNT(f)
           FROM CourseFeedback f
           WHERE f.course.id = :courseId AND f.deletedFlag = false
           """)
    Object[] calcRatingStats(@Param("courseId") Long courseId);
}

