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
public class CourseCompletionCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private Long enrollmentId; // Link đến enrollment

    @Column(name = "user_id", nullable = false)
    private Long userId; // Learner

    @Column(name = "course_id", nullable = false)
    private Long courseId; // Course đã hoàn thành

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
            certificateNumber = String.format("CERT-%d-%d-%d", courseId, userId, timestamp);
        }
    }
}

