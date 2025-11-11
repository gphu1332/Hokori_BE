package com.hokori.web.repository;

import com.hokori.web.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz_IdOrderByOrderIndexAsc(Long quizId);
    long countByQuiz_Id(Long quizId);
}
