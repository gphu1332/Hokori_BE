// com.hokori.web.entity.JlptTest.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "jlpt_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc event JLPT nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_test_event")
    )
    private JlptEvent event;

    // Moderator tạo đề
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_test_user")
    )
    private User createdBy;

    @Column(nullable = false, length = 10)
    private String level; // N5, N4,...

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // Ghi chú rule kết quả / mô tả, đúng với column "result" trong diagram
    @Column(name = "result", length = 500)
    private String result;

    // Temporarily allow nullable to avoid schema validation error with existing NULL values
    // Will be set to nullable = false after data migration
    @Column(name = "current_participants", nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        // Ensure currentParticipants is never null (handle existing NULL values from database)
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        // Ensure currentParticipants is never null
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
    }

    @PostLoad
    void postLoad() {
        // Fix NULL values when reading from PostgreSQL database
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
    }
}
