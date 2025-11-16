// com.hokori.web.entity.FlashcardSet.java
package com.hokori.web.entity;
import com.hokori.web.Enum.FlashcardSetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "flashcard_sets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FlashcardSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người tạo (learner hoặc teacher)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id",
            foreignKey = @ForeignKey(name = "fk_flashcard_set_user"))
    private User createdBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 50)
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private FlashcardSetType type;

    /**
     * OPTIONAL:
     * - Với set PERSONAL: null
     * - Với set COURSE_VOCAB: trỏ tới SectionsContent (session từ vựng)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "section_content_id",
            foreignKey = @ForeignKey(name = "fk_flashcard_set_section_content")
    )
    private SectionsContent sectionContent;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "set", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Flashcard> cards;

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
}
