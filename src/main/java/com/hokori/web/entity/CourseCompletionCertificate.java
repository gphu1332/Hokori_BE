package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Certificate tự động tạo khi learner hoàn thành 100% course
 * Đây là accomplishment/badge cho learner, không phải certificate chính thức
 */
@Entity
@Table(name = "course_completion_certificates",
        indexes = {
                @Index(name = "idx_cert_enrollment_id", columnList = "enrollment_id"),
                @Index(name = "idx_cert_user_id", columnList = "user_id"),
                @Index(name = "idx_cert_course_id", columnList = "course_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cert_enrollment", columnNames = {"enrollment_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "course", "enrollment"}) // Exclude relationships để tránh LazyInitializationException
public class CourseCompletionCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "enrollment_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_cc_enrollment")
    )
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_course")
    )
    private Course course;

    // Convenience methods để không break code hiện tại
    public Long getEnrollmentId() {
        return enrollment != null ? enrollment.getId() : null;
    }

    public void setEnrollmentId(Long enrollmentId) {
        if (enrollmentId != null) {
            this.enrollment = new Enrollment();
            this.enrollment.setId(enrollmentId);
        } else {
            this.enrollment = null;
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

    @Column(name = "course_title", length = 500)
    private String courseTitle; // Snapshot tên course khi hoàn thành

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt; // Thời điểm hoàn thành (từ enrollment.completedAt)

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt; // Thời điểm certificate được tạo

    @Column(name = "certificate_number", length = 100, unique = true)
    private String certificateNumber; // Số certificate (format: CERT-{courseId}-{userId}-{timestamp})

    @PrePersist
    void prePersist() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
        if (certificateNumber == null) {
            // Generate certificate number: CERT-{courseId}-{userId}-{timestamp}
            long timestamp = Instant.now().toEpochMilli();
            Long courseIdValue = getCourseId();
            Long userIdValue = getUserId();
            certificateNumber = String.format("CERT-%d-%d-%d", 
                courseIdValue != null ? courseIdValue : 0, 
                userIdValue != null ? userIdValue : 0, 
                timestamp);
        }
    }
}

