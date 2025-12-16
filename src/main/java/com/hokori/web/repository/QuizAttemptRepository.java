package com.hokori.web.repository;

import com.hokori.web.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUser_IdAndQuiz_IdOrderByStartedAtDesc(Long userId, Long quizId);

    Optional<QuizAttempt> findByIdAndUser_Id(Long id, Long userId);

    @Query("""
        select count(q) from Question q where q.quiz.id = :quizId
    """)
    int countQuestions(@Param("quizId") Long quizId);
    
    long countByUser_IdAndQuiz_Id(Long userId, Long quizId);
}
