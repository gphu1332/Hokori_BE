package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "options",
        indexes = {
                @Index(name="idx_options_question_id", columnList="question_id")
        })
@Getter @Setter
public class Option {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_options_question"))
    private Question question;

    @Column(columnDefinition="TEXT", nullable=false)
    @JsonIgnore // Prevent serialization to avoid LOB stream errors (use DTO/mapper instead)
    private String content;

    @Column(name="is_correct", nullable=false)
    private Boolean isCorrect = false;

    @Column(name="order_index", nullable=false)
    private Integer orderIndex = 1;

    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;

    @PrePersist void prePersist(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preUpdate(){  updatedAt = LocalDateTime.now(); }
}

