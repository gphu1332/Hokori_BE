package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "course_comment",
        indexes = {
                @Index(name = "ix_cc_course", columnList = "course_id"),
                @Index(name = "ix_cc_course_parent", columnList = "course_id,parent_id,created_at")
        }
)
@Getter
@Setter
public class CourseComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // OK cho cả SQL Server & Postgres
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_course")
    )
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_user")
    )
    private User user;

    // self-reference để reply
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_cc_parent")
    )
    private CourseComment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CourseComment> replies = new ArrayList<>();

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "is_edited", nullable = false)
    private boolean edited = false;

    // deletedFlag, createdAt, updatedAt kế thừa từ BaseEntity
}
