package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "user_content_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ucp_enrollment_content",
                columnNames = {"enrollment_id", "content_id"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserContentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Enrollment (user + course)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ucp_enrollment"))
    private Enrollment enrollment;

    // Content cụ thể trong section
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ucp_content"))
    private SectionsContent content;

    @Builder.Default
    @Column(name = "last_position_sec", nullable = false)
    private Long lastPositionSec = 0L;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    void onCreate() {
        // Phòng khi object tạo bằng constructor thường, hoặc builder set null
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (lastPositionSec == null) lastPositionSec = 0L;
        if (isCompleted == null) isCompleted = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
