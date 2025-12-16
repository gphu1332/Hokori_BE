package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "enrollment", uniqueConstraints =
@UniqueConstraint(name = "uk_enroll_user_course", columnNames = {"user_id", "course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"user", "course"}) // Exclude relationships để tránh LazyInitializationException
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // JPA Relationship với Course
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_course")
    )
    private Course course;

    // JPA Relationship với User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_user")
    )
    private User user;

    // Convenience methods để không break code hiện tại
    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    public void setCourseId(Long courseId) {
        if (courseId != null) {
            this.course = new Course();
            this.course.setId(courseId);
        } else {
            this.course = null;
        }
    }

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

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_access_at")
    private Instant lastAccessAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
