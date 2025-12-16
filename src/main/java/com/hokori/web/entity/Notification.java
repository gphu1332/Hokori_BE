package com.hokori.web.entity;

import com.hokori.web.Enum.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Entity lưu notifications cho users
 */
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_id", columnList = "user_id"),
                @Index(name = "idx_notification_read", columnList = "is_read"),
                @Index(name = "idx_notification_created_at", columnList = "created_at")
        })
@Getter
@Setter
@ToString(exclude = {"user"}) // Exclude relationships để tránh LazyInitializationException
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_user")
    )
    private User user;

    // Convenience methods để không break code hiện tại
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        if (userId != null) {
            this.user = new User();
            this.user.setId(userId);
        } else {
            this.user = null;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title; // Tiêu đề notification

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Nội dung chi tiết

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false; // Đã đọc chưa

    @Column(name = "read_at")
    private Instant readAt; // Thời điểm đọc

    // Related entity IDs (optional, để link đến detail page)
    @Column(name = "related_course_id")
    private Long relatedCourseId; // Link đến course (nếu là notification về course)

    @Column(name = "related_payment_id")
    private Long relatedPaymentId; // Link đến payment (nếu là notification về payment)

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

