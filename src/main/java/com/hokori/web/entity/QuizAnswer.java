package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
@Entity @Table(name = "quiz_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_question", columnNames = {"attempt_id","question_id"}))
public class QuizAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Với SINGLE_CHOICE chỉ cần option id */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "option_id")
    private Option option;

    /** Chấm đúng/sai tại thời điểm submit (điền khi submit) */
    @Column(name = "is_correct")
    private Boolean isCorrect;
}
