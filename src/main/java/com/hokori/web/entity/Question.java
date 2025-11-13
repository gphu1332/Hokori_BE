package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions",
        indexes = {
                @Index(name="idx_questions_quiz_id", columnList="quiz_id")
        })
@Getter @Setter
public class Question {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_questions_quiz"))
    private Quiz quiz;

    @Column(columnDefinition="TEXT", nullable=false)
    @JsonIgnore // Prevent serialization to avoid LOB stream errors (use DTO/mapper instead)
    private String content;

    @Column(name="question_type", length = 30, nullable=false)
    private String questionType = "SINGLE_CHOICE";

    @Column(columnDefinition="TEXT")
    @JsonIgnore // Prevent serialization to avoid LOB stream errors (use DTO/mapper instead)
    private String explanation;

    @Column(name="order_index", nullable=false)
    private Integer orderIndex = 1;

    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="deleted_flag") private Boolean deletedFlag = false;

    @PrePersist void prePersist(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preUpdate(){  updatedAt = LocalDateTime.now(); }
}

