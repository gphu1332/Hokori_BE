// com.hokori.web.entity.Flashcard.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "flashcards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id",
            foreignKey = @ForeignKey(name = "fk_flashcard_set"))
    private FlashcardSet set;

    @Column(name = "front_text", nullable = false, length = 255)
    private String frontText;

    @Column(name = "back_text", nullable = false, length = 255)
    private String backText;

    @Column(length = 255)
    private String reading;

    @Column(name = "example_sentence", length = 1000)
    private String exampleSentence;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

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
}
