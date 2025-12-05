// com.hokori.web.entity.JlptTestAttemptAnswer.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity để lưu chi tiết đáp án của mỗi lần làm bài JLPT Test (attempt).
 * Mỗi attempt có nhiều answers, mỗi answer tương ứng với 1 câu hỏi.
 */
@Entity
@Table(
        name = "jlpt_test_attempt_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jlpt_attempt_answer_attempt_question",
                columnNames = {"attempt_id", "question_id"}
        ),
        indexes = {
                @Index(name = "idx_jlpt_attempt_answer_attempt", columnList = "attempt_id"),
                @Index(name = "idx_jlpt_attempt_answer_question", columnList = "question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptTestAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc attempt nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attempt_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_attempt_answer_attempt")
    )
    private JlptTestAttempt attempt;

    // Câu hỏi nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_attempt_answer_question")
    )
    private JlptQuestion question;

    // Option mà user đã chọn (null nếu user không chọn đáp án)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "selected_option_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_jlpt_attempt_answer_selected_option")
    )
    private JlptOption selectedOption;

    // Option đúng (để FE hiển thị đáp án đúng)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "correct_option_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_attempt_answer_correct_option")
    )
    private JlptOption correctOption;

    // Đáp án đúng hay sai
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

