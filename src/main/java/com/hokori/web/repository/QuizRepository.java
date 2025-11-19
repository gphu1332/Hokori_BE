package com.hokori.web.repository;

import com.hokori.web.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByLesson_Id(Long lessonId);
    
    /**
     * Get quiz metadata with description (avoids LOB stream error on PostgreSQL).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, lessonId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
     */
    @Query(value = """
        SELECT q.id, q.lesson_id, q.title, q.description, q.total_questions, 
               q.time_limit_sec, q.pass_score_percent, q.created_at, q.updated_at, q.deleted_flag
        FROM quizzes q
        WHERE q.id = :id
        """, nativeQuery = true)
    Optional<Object[]> findQuizMetadataById(@Param("id") Long id);
    
    /**
     * Get quiz metadata by lesson ID (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, lessonId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
     */
    @Query(value = """
        SELECT q.id, q.lesson_id, q.title, q.description, q.total_questions, 
               q.time_limit_sec, q.pass_score_percent, q.created_at, q.updated_at, q.deleted_flag
        FROM quizzes q
        WHERE q.lesson_id = :lessonId AND (q.deleted_flag IS NULL OR q.deleted_flag = false)
        """, nativeQuery = true)
    Optional<Object[]> findQuizMetadataByLessonId(@Param("lessonId") Long lessonId);
}

