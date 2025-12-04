package com.hokori.web.repository;

import com.hokori.web.entity.JlptUserTestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JlptUserTestSessionRepository
        extends JpaRepository<JlptUserTestSession, Long> {

    Optional<JlptUserTestSession> findByTest_IdAndUser_Id(Long testId, Long userId);

    long countByTest_IdAndExpiresAtAfter(Long testId, Instant now);

    /**
     * Upsert session: Insert if not exists, update if exists
     * Uses PostgreSQL ON CONFLICT to handle race conditions atomically
     * Note: created_at is only set on INSERT, not updated on conflict (due to updatable=false constraint)
     */
    @Modifying
    @Query(value = """
        INSERT INTO jlpt_user_test_session (test_id, user_id, started_at, expires_at, created_at, updated_at, deleted_flag)
        VALUES (:testId, :userId, :startedAt, :expiresAt, :now, :now, false)
        ON CONFLICT (test_id, user_id)
        DO UPDATE SET
            started_at = :startedAt,
            expires_at = :expiresAt,
            updated_at = :now
        """, nativeQuery = true)
    void upsertSession(
            @Param("testId") Long testId,
            @Param("userId") Long userId,
            @Param("startedAt") Instant startedAt,
            @Param("expiresAt") Instant expiresAt,
            @Param("now") Instant now
    );
}
