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
public class TeacherRevenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId; // Teacher owner của course
    
    @Column(name = "course_id", nullable = false)
    private Long courseId; // Course được bán
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId; // Payment tạo ra revenue này
    
    @Column(name = "enrollment_id")
    private Long enrollmentId; // Enrollment được tạo từ payment này
    
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

