package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity để track mỗi lần verify OTP sai
 * Đơn giản: mỗi lần verify sai → insert một record
 * Đếm số record trong 15 phút gần đây, nếu >= 5 → lockout
 */
@Data
@Entity
@Table(name = "password_reset_failed_attempts",
        indexes = {
                @Index(name = "idx_failed_attempts_email", columnList = "email"),
                @Index(name = "idx_failed_attempts_email_attempted", columnList = "email, attempted_at")
        })
public class PasswordResetFailedAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "ip_address", length = 45, nullable = true)
    private String ipAddress;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "otp_id", nullable = true)
    private Long otpId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (attemptedAt == null) {
            attemptedAt = createdAt;
        }
    }
}

