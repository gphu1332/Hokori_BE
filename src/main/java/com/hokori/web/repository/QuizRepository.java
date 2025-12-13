package com.hokori.web.repository;

import com.hokori.web.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findBySection_Id(Long sectionId);
    
    /**
     * Get quiz metadata with description (avoids LOB stream error on PostgreSQL).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, sectionId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
     */
    @Query(value = """
        SELECT q.id, q.section_id, q.title, q.description, q.total_questions, 
               q.time_limit_sec, q.pass_score_percent, q.created_at, q.updated_at, q.deleted_flag
        FROM quizzes q
        WHERE q.id = :id
        """, nativeQuery = true)
    Optional<Object[]> findQuizMetadataById(@Param("id") Long id);
    
    /**
     * Get quiz metadata by section ID (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, sectionId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
     * Only returns non-deleted quizzes.
     */
    @Query(value = """
        SELECT q.id, q.section_id, q.title, q.description, q.total_questions, 
               q.time_limit_sec, q.pass_score_percent, q.created_at, q.updated_at, q.deleted_flag
        FROM quizzes q
        WHERE q.section_id = :sectionId AND (q.deleted_flag IS NULL OR q.deleted_flag = false)
        ORDER BY q.created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Object[]> findQuizMetadataBySectionId(@Param("sectionId") Long sectionId);
    
    /**
     * Find all quizzes (including deleted) for a section - for debugging
     */
    @Query(value = """
        SELECT q.id, q.section_id, q.title, q.description, q.total_questions, 
               q.time_limit_sec, q.pass_score_percent, q.created_at, q.updated_at, q.deleted_flag
        FROM quizzes q
        WHERE q.section_id = :sectionId
        ORDER BY q.created_at DESC
        """, nativeQuery = true)
    List<Object[]> findAllQuizMetadataBySectionId(@Param("sectionId") Long sectionId);
}

