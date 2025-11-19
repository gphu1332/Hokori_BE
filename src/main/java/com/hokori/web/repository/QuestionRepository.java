package com.hokori.web.repository;

import com.hokori.web.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz_IdOrderByOrderIndexAsc(Long quizId);
    long countByQuiz_Id(Long quizId);
    
    /**
     * Get question metadata with content (avoids LOB stream error on PostgreSQL).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
     */
    @Query(value = """
        SELECT q.id, q.quiz_id, q.content, q.question_type, q.explanation, 
               q.order_index, q.created_at, q.updated_at, q.deleted_flag
        FROM questions q
        WHERE q.id = :id
        """, nativeQuery = true)
    Optional<Object[]> findQuestionMetadataById(@Param("id") Long id);
    
    /**
     * Get questions metadata for a quiz (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
     */
    @Query(value = """
        SELECT q.id, q.quiz_id, q.content, q.question_type, q.explanation, 
               q.order_index, q.created_at, q.updated_at, q.deleted_flag
        FROM questions q
        WHERE q.quiz_id = :quizId AND (q.deleted_flag IS NULL OR q.deleted_flag = false)
        ORDER BY q.order_index ASC
        """, nativeQuery = true)
    List<Object[]> findQuestionMetadataByQuizId(@Param("quizId") Long quizId);
}
