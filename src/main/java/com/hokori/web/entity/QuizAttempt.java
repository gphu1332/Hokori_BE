package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "quiz_attempts",
        indexes = {
                @Index(name="idx_attempt_user", columnList = "user_id"),
                @Index(name="idx_attempt_quiz", columnList = "quiz_id")
        })
public class QuizAttempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ai làm bài */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Quiz thuộc lesson này */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** Trạng thái */
    @Enumerated(EnumType.STRING) @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Kết quả chấm */
    @Column(name = "score_percent")
    private Integer scorePercent; // 0..100

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    public enum Status { IN_PROGRESS, SUBMITTED, CANCELLED }
}
