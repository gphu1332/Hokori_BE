package com.hokori.web.repository;

import com.hokori.web.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByLesson_Id(Long lessonId);
}

