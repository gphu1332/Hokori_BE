package com.hokori.web.repository;

import com.hokori.web.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    long countByChapter_Id(Long chapterId);
}
