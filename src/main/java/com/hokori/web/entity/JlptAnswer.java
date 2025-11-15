// com.hokori.web.entity.JlptAnswer.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "jlpt_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jlpt_answer_user_test_question",
                columnNames = {"user_id", "test_id", "question_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User nào làm bài
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_answer_user")
    )
    private User user;

    // Thuộc đề nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_answer_test")
    )
    private JlptTest test;

    // Câu hỏi nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_answer_question")
    )
    private JlptQuestion question;

    // Option mà user chọn
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "selected_option_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_answer_option")
    )
    private JlptOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (answeredAt == null) {
            answeredAt = now;
        }
        createdAt = now;
    }
}
