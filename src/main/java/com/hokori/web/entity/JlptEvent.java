// com.hokori.web.entity.JlptEvent.java
package com.hokori.web.entity;

import com.hokori.web.Enum.JlptEventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jlpt_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin tạo event
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_event_user")
    )
    private User createdBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 10)
    private String level; // N5, N4,...

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JlptEventStatus status;

    // ==== đổi sang LocalDateTime cho khớp DTO ====
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = JlptEventStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
