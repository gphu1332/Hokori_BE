package com.hokori.web.repository;

import com.hokori.web.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    long countByLesson_Id(Long lessonId);
    @Query("select s.lesson.chapter.course.id from Section s where s.id = :sectionId")
    Optional<Long> findCourseIdBySectionId(@Param("sectionId") Long sectionId);

    List<Section> findByLesson_IdOrderByOrderIndexAsc(Long lessonId);
}
