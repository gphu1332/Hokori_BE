package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entity để track revenue của teacher theo tháng và theo course
 * Tự động tạo khi có payment thành công cho course
 */
@Entity
@Table(name = "teacher_revenue",
        indexes = {
                @Index(name = "idx_teacher_revenue_teacher", columnList = "teacher_id"),
                @Index(name = "idx_teacher_revenue_course", columnList = "course_id"),
                @Index(name = "idx_teacher_revenue_payment", columnList = "payment_id"),
                @Index(name = "idx_teacher_revenue_year_month", columnList = "year_month"),
                @Index(name = "idx_teacher_revenue_paid", columnList = "is_paid")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teacher_revenue_payment", columnNames = {"payment_id", "course_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"teacher", "course", "payment", "enrollment", "payoutBy"}) // Exclude relationships để tránh LazyInitializationException
public class TeacherRevenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // JPA Relationship với User (teacher)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "teacher_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_revenue_teacher")
    )
    private User teacher;
    
    // JPA Relationship với Course
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_revenue_course")
    )
    private Course course;
    
    // JPA Relationship với Payment
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_revenue_payment")
    )
    private Payment payment;
    
    // JPA Relationship với Enrollment (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "enrollment_id",
            foreignKey = @ForeignKey(name = "fk_teacher_revenue_enrollment")
    )
    private Enrollment enrollment;
    
    // JPA Relationship với User (payoutBy - admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payout_by_user_id",
            foreignKey = @ForeignKey(name = "fk_teacher_revenue_payout_by")
    )
    private User payoutBy;
    
    // Convenience methods để không break code hiện tại
    public Long getTeacherId() {
        return teacher != null ? teacher.getId() : null;
    }

    public void setTeacherId(Long teacherId) {
        if (teacherId != null) {
            this.teacher = new User();
            this.teacher.setId(teacherId);
        } else {
            this.teacher = null;
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

    public Long getPaymentId() {
        return payment != null ? payment.getId() : null;
    }

    public void setPaymentId(Long paymentId) {
        if (paymentId != null) {
            this.payment = new Payment();
            this.payment.setId(paymentId);
        } else {
            this.payment = null;
        }
    }

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

    public Long getPayoutByUserId() {
        return payoutBy != null ? payoutBy.getId() : null;
    }

    public void setPayoutByUserId(Long payoutByUserId) {
        if (payoutByUserId != null) {
            this.payoutBy = new User();
            this.payoutBy.setId(payoutByUserId);
        } else {
            this.payoutBy = null;
        }
    }
    
    // Revenue calculation
    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents; // Tổng tiền từ payment (có thể là nhiều courses)
    
    @Column(name = "course_price_cents", nullable = false)
    private Long coursePriceCents; // Giá của course này trong payment
    
    @Column(name = "teacher_revenue_cents", nullable = false)
    private Long teacherRevenueCents; // 80% của coursePriceCents
    
    @Column(name = "admin_commission_cents", nullable = false)
    private Long adminCommissionCents; // 20% của coursePriceCents
    
    // Time tracking
    @Column(name = "year_month", nullable = false, length = 7) // Format: "2025-01"
    private String yearMonth; // Tháng tính revenue (YYYY-MM)
    
    @Column(name = "paid_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant paidAt = Instant.now(); // Thời gian payment thành công
    
    // Payout tracking
    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private Boolean isPaid = false; // Đã chuyển tiền cho teacher chưa
    
    @Column(name = "payout_date")
    private Instant payoutDate; // Thời gian admin đánh dấu đã chuyển tiền
    
    @Column(name = "payout_by_user_id")
    private Long payoutByUserId; // Admin đã đánh dấu chuyển tiền
    
    @Column(name = "payout_note", columnDefinition = "TEXT")
    private String payoutNote; // Ghi chú khi chuyển tiền
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

