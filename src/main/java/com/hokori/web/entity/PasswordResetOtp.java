package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity để lưu mã OTP cho password reset
 * Chỉ hỗ trợ email
 */
@Data
@Entity
@Table(name = "password_reset_otp",
        indexes = {
                @Index(name = "idx_otp_email", columnList = "email"),
                @Index(name = "idx_otp_code", columnList = "otp_code")
        })
@org.hibernate.annotations.DynamicUpdate
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email của user để reset password
     */
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    /**
     * Mã OTP (6 chữ số)
     */
    @Column(name = "otp_code", length = 6, nullable = false)
    private String otpCode;

    /**
     * Thời gian hết hạn (thường là 10-15 phút sau khi tạo)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Đã sử dụng chưa (để tránh reuse OTP)
     */
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    /**
     * Số lần verify sai (để chặn brute force)
     */
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    /**
     * Thời gian tạo OTP
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // Default: expire sau 15 phút
            expiresAt = createdAt.plusMinutes(15);
        }
    }
}

