// com.hokori.web.repository.JlptTestAttemptAnswerRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.JlptTestAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptTestAttemptAnswerRepository extends JpaRepository<JlptTestAttemptAnswer, Long> {
    
    /**
     * Lấy tất cả answers của một attempt, sắp xếp theo orderIndex của question.
     */
    List<JlptTestAttemptAnswer> findByAttempt_IdOrderByQuestion_OrderIndexAsc(Long attemptId);
}

