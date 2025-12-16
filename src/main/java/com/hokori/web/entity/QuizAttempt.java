package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "quiz_attempts",
        indexes = {
                // Note: Index names are prefixed with table name to avoid conflicts
                @Index(name="idx_quiz_attempt_user", columnList = "user_id"),
                @Index(name="idx_quiz_attempt_quiz", columnList = "quiz_id")
        })
@ToString(exclude = {"user", "quiz"}) // Exclude relationships để tránh LazyInitializationException
public class QuizAttempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ai làm bài */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_quiz_attempt_user")
    )
    private User user;

    /** Quiz thuộc lesson này */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "quiz_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_quiz_attempt_quiz")
    )
    private Quiz quiz;

    // Convenience methods để không break code hiện tại
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        if (userId != null) {
            this.user = new User();
            this.user.setId(userId);
        } else {
            this.user = null;
        }
    }

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
