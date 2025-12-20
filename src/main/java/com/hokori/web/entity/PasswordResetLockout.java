package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity để track lockout cho password reset khi brute-force attack
 * Khóa chức năng forgot password cho email/IP trong một khoảng thời gian
 */
@Data
@Entity
@Table(name = "password_reset_lockout",
        indexes = {
                @Index(name = "idx_lockout_email", columnList = "email"),
                @Index(name = "idx_lockout_ip", columnList = "ip_address"),
                @Index(name = "idx_lockout_unlock_at", columnList = "unlock_at")
        })
public class PasswordResetLockout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email bị khóa (nullable - có thể khóa theo IP mà không cần email)
     */
    @Column(name = "email", length = 255, nullable = true)
    private String email;

    /**
     * IP address bị khóa (nullable - có thể khóa theo email mà không cần IP)
     */
    @Column(name = "ip_address", length = 45, nullable = true) // IPv6 max length is 45
    private String ipAddress;

    /**
     * Thời gian bắt đầu khóa
     */
    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    /**
     * Thời gian mở khóa (thường là 30 phút sau khi locked_at)
     */
    @Column(name = "unlock_at", nullable = false)
    private LocalDateTime unlockAt;

    /**
     * Lý do khóa (ví dụ: "Too many failed OTP attempts")
     */
    @Column(name = "reason", length = 500, nullable = true)
    private String reason;

    /**
     * Đã mở khóa chưa (có thể mở khóa thủ công trước khi hết hạn)
     */
    @Column(name = "is_unlocked", nullable = false)
    private Boolean isUnlocked = false;

    /**
     * Thời gian tạo record
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lockedAt == null) {
            lockedAt = createdAt;
        }
        if (unlockAt == null) {
            // Default: unlock sau 30 phút
            unlockAt = lockedAt.plusMinutes(30);
        }
    }
}

