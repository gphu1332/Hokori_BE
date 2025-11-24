// com.hokori.web.entity.JlptQuestion.java
package com.hokori.web.entity;

import com.hokori.web.Enum.JlptQuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "jlpt_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc đề nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_question_test")
    )
    private JlptTest test;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private JlptQuestionType questionType;

    @Column(length = 2000)
    private String explanation; // lời giải / giải thích

    @Column(name = "order_index")
    private Integer orderIndex;

    // ===== Media bằng file path (không dùng Asset) =====

    @Column(name = "audio_path", length = 500)
    private String audioPath;   // audio cho câu nghe

    @Column(name = "image_path", length = 500)
    private String imagePath;   // hình minh họa câu hỏi

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "image_alt_text", length = 255)
    private String imageAltText;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
