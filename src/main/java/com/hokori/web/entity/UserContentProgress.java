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
public class UserContentProgress extends BaseEntity { // dùng created_at/updated_at từ BaseEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ucp_enrollment"))
    private Enrollment enrollment;

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
}
