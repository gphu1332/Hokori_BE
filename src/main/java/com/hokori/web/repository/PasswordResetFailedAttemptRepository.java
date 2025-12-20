package com.hokori.web.repository;

import com.hokori.web.entity.PasswordResetFailedAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PasswordResetFailedAttemptRepository extends JpaRepository<PasswordResetFailedAttempt, Long> {

    /**
     * Đếm số lần verify sai OTP của một email trong khoảng thời gian gần đây (15 phút)
     */
    @Query("""
            SELECT COUNT(f) 
            FROM PasswordResetFailedAttempt f
            WHERE f.email = :email
            AND f.attemptedAt > :since
            """)
    Long countFailedAttemptsByEmailSince(
            @Param("email") String email,
            @Param("since") LocalDateTime since);
}

