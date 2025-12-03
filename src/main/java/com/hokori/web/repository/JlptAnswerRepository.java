package com.hokori.web.repository;

import com.hokori.web.entity.JlptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JlptAnswerRepository extends JpaRepository<JlptAnswer, Long> {
    java.util.Optional<JlptAnswer> findByUser_IdAndTest_IdAndQuestion_Id(Long userId, Long testId, Long questionId);
    Long countByUser_IdAndTest_IdAndIsCorrectTrue(Long userId, Long testId);

    void deleteByUser_IdAndTest_Id(Long userId, Long testId);
    
    // Get all answers for a user and test
    List<JlptAnswer> findByUser_IdAndTest_Id(Long userId, Long testId);
    
    // Count correct answers by question type
    @Query("SELECT COUNT(a) FROM JlptAnswer a " +
           "WHERE a.user.id = :userId AND a.test.id = :testId " +
           "AND a.isCorrect = true " +
           "AND a.question.questionType IN :types")
    Long countCorrectByUserAndTestAndQuestionTypes(
            @Param("userId") Long userId,
            @Param("testId") Long testId,
            @Param("types") java.util.List<com.hokori.web.Enum.JlptQuestionType> types
    );

    /**
     * Upsert answer using PostgreSQL ON CONFLICT UPDATE
     * This handles race conditions at database level
     * Note: created_at is only set on INSERT, not updated on conflict (due to updatable=false constraint)
     */
    @Modifying
    @Query(value = """
        INSERT INTO jlpt_answers (user_id, test_id, question_id, selected_option_id, is_correct, answered_at, created_at)
        VALUES (:userId, :testId, :questionId, :selectedOptionId, :isCorrect, :answeredAt, :createdAt)
        ON CONFLICT (user_id, test_id, question_id)
        DO UPDATE SET
            selected_option_id = EXCLUDED.selected_option_id,
            is_correct = EXCLUDED.is_correct,
            answered_at = EXCLUDED.answered_at
        """, nativeQuery = true)
    void upsertAnswer(
            @Param("userId") Long userId,
            @Param("testId") Long testId,
            @Param("questionId") Long questionId,
            @Param("selectedOptionId") Long selectedOptionId,
            @Param("isCorrect") Boolean isCorrect,
            @Param("answeredAt") Instant answeredAt,
            @Param("createdAt") Instant createdAt
    );
}
