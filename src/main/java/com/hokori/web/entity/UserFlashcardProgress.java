// com.hokori.web.entity.UserFlashcardProgress.java
package com.hokori.web.entity;

import com.hokori.web.Enum.FlashcardProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "user_flashcard_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_flashcard_progress_user_card",
                columnNames = {"user_id", "flashcard_id"}
        ),
        indexes = {
                @Index(name = "ix_ufp_user", columnList = "user_id"),
                @Index(name = "ix_ufp_last_reviewed", columnList = "last_reviewed_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserFlashcardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ufp_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "flashcard_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ufp_flashcard")
    )
    private Flashcard flashcard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FlashcardProgressStatus status = FlashcardProgressStatus.NEW;

    @Column(name = "mastered_at")
    private Instant masteredAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }


    // convenience method, không map DB
    public boolean isMastered() {
        return this.status == FlashcardProgressStatus.MASTERED;
    }
}
