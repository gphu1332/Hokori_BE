package com.hokori.web.entity;

import com.hokori.web.Enum.FlagType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity lưu các flag/report của user về course
 */
@Entity
@Table(name = "course_flag",
        indexes = {
                @Index(name = "idx_course_flag_course_id", columnList = "course_id"),
                @Index(name = "idx_course_flag_user_id", columnList = "user_id"),
                @Index(name = "idx_course_flag_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class CourseFlag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_flag_course"))
    private Course course;

    @Column(name = "user_id", nullable = false)
    private Long userId; // User flag course

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 50)
    private FlagType flagType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Lý do chi tiết (optional)
    
    // Note: createdAt, updatedAt, deletedFlag được kế thừa từ BaseEntity
}

