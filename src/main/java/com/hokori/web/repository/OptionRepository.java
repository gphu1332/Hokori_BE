package com.hokori.web.repository;

import com.hokori.web.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByQuestion_IdOrderByOrderIndexAsc(Long questionId);
    long countByQuestion_IdAndIsCorrectTrue(Long questionId);
    
    /**
     * Get option metadata with content (avoids LOB stream error on PostgreSQL).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
     */
    @Query(value = """
        SELECT o.id, o.question_id, o.content, o.is_correct, o.order_index, 
               o.created_at, o.updated_at
        FROM options o
        WHERE o.id = :id
        """, nativeQuery = true)
    Optional<Object[]> findOptionMetadataById(@Param("id") Long id);
    
    /**
     * Get options metadata for a question (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
     */
    @Query(value = """
        SELECT o.id, o.question_id, o.content, o.is_correct, o.order_index, 
               o.created_at, o.updated_at
        FROM options o
        WHERE o.question_id = :questionId
        ORDER BY o.order_index ASC
        """, nativeQuery = true)
    List<Object[]> findOptionMetadataByQuestionId(@Param("questionId") Long questionId);
}

