package com.hokori.web.repository;

import com.hokori.web.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    long countByChapter_Id(Long chapterId);

    @Query("select ls.chapter.course.id from Lesson ls where ls.id = :lessonId")
    Optional<Long> findCourseIdByLessonId(@Param("lessonId") Long lessonId);

    @Query("select ls.chapter.id from Lesson ls where ls.id = :lessonId")
    Optional<Long> findChapterIdByLessonId(@Param("lessonId") Long lessonId);

    List<Lesson> findByChapter_IdOrderByOrderIndexAsc(Long chapterId);

    // Lấy owner userId của course chứa lesson này (JPQL dùng field userId)
    @Query("""
           select l.chapter.course.userId
           from Lesson l
           where l.id = :lessonId
           """)
    Optional<Long> findCourseOwnerIdByLessonId(@Param("lessonId") Long lessonId);
}
