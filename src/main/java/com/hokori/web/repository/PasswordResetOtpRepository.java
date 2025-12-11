package com.hokori.web.repository;

import com.hokori.web.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    /**
     * Tìm OTP chưa hết hạn và chưa sử dụng theo email
     */
    @Query("""
            SELECT o FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.isUsed = false
            AND o.expiresAt > :now
            ORDER BY o.createdAt DESC
            """)
    Optional<PasswordResetOtp> findLatestValidByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Tìm OTP chưa hết hạn và chưa sử dụng theo phone number
     */
    @Query("""
            SELECT o FROM PasswordResetOtp o
            WHERE o.phoneNumber = :phoneNumber
            AND o.isUsed = false
            AND o.expiresAt > :now
            ORDER BY o.createdAt DESC
            """)
    Optional<PasswordResetOtp> findLatestValidByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("now") LocalDateTime now);

    /**
     * Đánh dấu OTP đã sử dụng
     */
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.isUsed = true WHERE o.id = :id")
    void markAsUsed(@Param("id") Long id);

    /**
     * Tăng số lần verify sai
     */
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.failedAttempts = o.failedAttempts + 1 WHERE o.id = :id")
    void incrementFailedAttempts(@Param("id") Long id);

    /**
     * Xóa các OTP đã hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.expiresAt < :now")
    void deleteExpiredOtp(@Param("now") LocalDateTime now);
}

