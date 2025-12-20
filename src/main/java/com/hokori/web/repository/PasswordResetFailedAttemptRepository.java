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
     * Sử dụng native query để bypass JPA cache và đảm bảo đếm đúng từ database
     */
    @Query(value = """
            SELECT COUNT(*) 
            FROM password_reset_failed_attempts
            WHERE email = :email
            AND attempted_at >= :since
            """, nativeQuery = true)
    Long countFailedAttemptsByEmailSince(
            @Param("email") String email,
            @Param("since") LocalDateTime since);
}

