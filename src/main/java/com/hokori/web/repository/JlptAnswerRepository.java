package com.hokori.web.repository;

import com.hokori.web.entity.JlptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JlptAnswerRepository extends JpaRepository<JlptAnswer, Long> {
    java.util.Optional<JlptAnswer> findByUser_IdAndTest_IdAndQuestion_Id(Long userId, Long testId, Long questionId);
    Long countByUser_IdAndTest_IdAndIsCorrectTrue(Long userId, Long testId);

    void deleteByUser_IdAndTest_Id(Long userId, Long testId);
}
