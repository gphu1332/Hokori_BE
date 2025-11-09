package com.hokori.web.entity;

import com.hokori.web.entity.Lesson;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes",
        indexes = {
                @Index(name="idx_quizzes_lesson_id", columnList="lesson_id")
        })
@Getter @Setter
public class Quiz {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quizzes_lesson"))
    private Lesson lesson;

    @Column(nullable=false) private String title;
    @Column(columnDefinition = "TEXT") private String description;

    @Column(name="total_questions", nullable=false)
    private Integer totalQuestions = 0;

    @Column(name="time_limit_sec") private Integer timeLimitSec;       // null = unlimited
    @Column(name="pass_score_percent") private Integer passScorePercent; // 0..100

    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="deleted_flag") private Boolean deletedFlag = false;

    @PrePersist void prePersist(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preUpdate(){  updatedAt = LocalDateTime.now(); }
}

