package com.hokori.web.repository;

import com.hokori.web.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByAttempt_Id(Long attemptId);

    @Query("""
        select a from QuizAnswer a
        where a.attempt.id = :attemptId and a.question.id = :questionId
    """)
    Optional<QuizAnswer> findOne(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId);

    @Query("""
        select q from Question q
        where q.quiz.id = :quizId and q.id not in
            (select a.question.id from QuizAnswer a where a.attempt.id = :attemptId)
        order by q.orderIndex asc
    """)
    List<Question> findUnanswered(@Param("quizId") Long quizId, @Param("attemptId") Long attemptId);
    
    /**
     * Get unanswered question IDs (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: List of question IDs
     */
    @Query(value = """
        SELECT q.id
        FROM questions q
        WHERE q.quiz_id = :quizId 
          AND (q.deleted_flag IS NULL OR q.deleted_flag = false)
          AND q.id NOT IN (
              SELECT a.question_id FROM quiz_answers a WHERE a.attempt_id = :attemptId
          )
        ORDER BY q.order_index ASC
        """, nativeQuery = true)
    List<Long> findUnansweredQuestionIds(@Param("quizId") Long quizId, @Param("attemptId") Long attemptId);
}