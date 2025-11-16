// com.hokori.web.entity.JlptOption.java
package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "jlpt_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc câu hỏi nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_jlpt_option_question")
    )
    private JlptQuestion question;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    // A/B/C/D...
    @Column(name = "order_index")
    private Integer orderIndex;

    // Đáp án dạng hình (optional)
    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_alt_text", length = 255)
    private String imageAltText;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
