package com.hokori.web.repository;

import com.hokori.web.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    long countByCourse_Id(Long courseId);
    long countByCourse_IdAndIsTrialTrue(Long courseId);
    Optional<Chapter> findByCourse_IdAndIsTrialTrue(Long courseId);

    @Query("select ch.course.id from Chapter ch where ch.id = :chapterId")
    Optional<Long> findCourseIdByChapterId(@Param("chapterId") Long chapterId);

    List<Chapter> findByCourse_IdOrderByOrderIndexAsc(Long courseId);


}

