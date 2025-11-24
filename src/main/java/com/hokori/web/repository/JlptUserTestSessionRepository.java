package com.hokori.web.repository;

import com.hokori.web.entity.JlptUserTestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JlptUserTestSessionRepository
        extends JpaRepository<JlptUserTestSession, Long> {

    Optional<JlptUserTestSession> findByTest_IdAndUser_Id(Long testId, Long userId);
}
