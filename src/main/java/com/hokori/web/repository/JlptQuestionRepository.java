package com.hokori.web.repository;

import com.hokori.web.Enum.JlptQuestionType;
import com.hokori.web.entity.JlptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptQuestionRepository extends JpaRepository<JlptQuestion, Long> {
    List<JlptQuestion> findByTest_IdAndDeletedFlagFalseOrderByOrderIndexAsc(Long testId);
    Long countByTest_IdAndDeletedFlagFalse(Long testId);

    // MỚI 1: chỉ 1 type (LISTENING / READING / GRAMMAR / VOCAB)
    List<JlptQuestion> findByTest_IdAndQuestionTypeAndDeletedFlagFalseOrderByOrderIndexAsc(
            Long testId,
            JlptQuestionType questionType
    );

    // MỚI 2: nhiều type (dùng cho GRAMMAR + VOCAB)
    List<JlptQuestion> findByTest_IdAndQuestionTypeInAndDeletedFlagFalseOrderByOrderIndexAsc(
            Long testId,
            java.util.List<JlptQuestionType> types
    );
}
