package com.hokori.web.repository;

import com.hokori.web.entity.JlptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptQuestionRepository extends JpaRepository<JlptQuestion, Long> {
    List<JlptQuestion> findByTest_IdAndDeletedFlagFalseOrderByOrderIndexAsc(Long testId);
    Long countByTest_IdAndDeletedFlagFalse(Long testId);
}
