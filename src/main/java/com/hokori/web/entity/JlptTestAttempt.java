// com.hokori.web.entity.JlptTestAttempt.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity để lưu kết quả mỗi lần user làm bài JLPT Test.
 * Mỗi lần user nộp bài sẽ tạo 1 attempt mới.
 * User có thể làm lại test nhiều lần, mỗi lần là 1 attempt riêng.
 */
@Entity
@Table(
        name = "jlpt_test_attempts",
        indexes = {
                @Index(name = "idx_attempt_user", columnList = "user_id"),
                @Index(name = "idx_attempt_test", columnList = "test_id"),
                @Index(name = "idx_attempt_submitted", columnList = "submitted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptTestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User nào làm bài
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_attempt_user")
    )
    private User user;

    // Test nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_attempt_test")
    )
    private JlptTest test;

    // Thời gian bắt đầu làm bài
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    // Thời gian nộp bài
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    // ====== KẾT QUẢ TỔNG ======
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "score", nullable = false)
    private Double score; // Tổng điểm

    @Column(name = "passed", nullable = false)
    private Boolean passed; // Đậu hay rớt

    // ====== ĐIỂM TỪNG PHẦN ======
    // Grammar + Vocab
    @Column(name = "grammar_vocab_total")
    private Integer grammarVocabTotal;

    @Column(name = "grammar_vocab_correct")
    private Integer grammarVocabCorrect;

    @Column(name = "grammar_vocab_score")
    private Double grammarVocabScore;

    // Reading
    @Column(name = "reading_total")
    private Integer readingTotal;

    @Column(name = "reading_correct")
    private Integer readingCorrect;

    @Column(name = "reading_score")
    private Double readingScore;

    // Listening
    @Column(name = "listening_total")
    private Integer listeningTotal;

    @Column(name = "listening_correct")
    private Integer listeningCorrect;

    @Column(name = "listening_score")
    private Double listeningScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

