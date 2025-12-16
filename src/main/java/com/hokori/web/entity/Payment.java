package com.hokori.web.entity;

import com.hokori.web.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment",
        indexes = {
                @Index(name = "idx_payment_user", columnList = "user_id"),
                @Index(name = "idx_payment_order_code", columnList = "order_code", unique = true),
                @Index(name = "idx_payment_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"}) // Exclude user để tránh LazyInitializationException
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode; // PayOS order code (unique)
    
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents; // Tổng tiền thanh toán (đơn vị: cent)
    
    @Column(name = "description", length = 500)
    private String description; // Mô tả đơn hàng
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
    
    // JPA Relationship với User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_user")
    )
    private User user;
    
    // Convenience method để lấy userId (không break code hiện tại)
    // Lombok @Getter sẽ không generate getUserId() vì đã có custom method này
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    // Convenience method để set userId (tạo User proxy để không break code hiện tại)
    // Lombok @Setter sẽ không generate setUserId() vì đã có custom method này
    // Note: Khi dùng builder, cần inject User entity thay vì chỉ userId
    public void setUserId(Long userId) {
        if (userId != null) {
            // Tạo User proxy để JPA có thể persist foreign key
            // Code sử dụng nên inject User entity thay vì chỉ userId
            this.user = new User();
            this.user.setId(userId);
        } else {
            this.user = null;
        }
    }
    
    @Column(name = "cart_id")
    private Long cartId; // Cart được thanh toán (optional)
    
    @Column(name = "course_ids", columnDefinition = "TEXT")
    private String courseIds; // JSON array of course IDs: [1, 2, 3]
    
    @Column(name = "ai_package_id")
    private Long aiPackageId; // AI Package ID (if this payment is for AI package)
    
    @Column(name = "ai_package_purchase_id")
    private Long aiPackagePurchaseId; // AIPackagePurchase ID (if this payment is for AI package)
    
    @Column(name = "payment_link", length = 1000)
    private String paymentLink; // Link thanh toán từ PayOS
    
    @Column(name = "payos_transaction_code")
    private String payosTransactionCode; // Transaction code từ PayOS (sau khi thanh toán thành công)
    
    @Column(name = "payos_qr_code", length = 1000)
    private String payosQrCode; // QR code từ PayOS (nếu có)
    
    @Column(name = "webhook_data", columnDefinition = "TEXT")
    private String webhookData; // Raw webhook data từ PayOS (JSON)
    
    @Column(name = "expired_at")
    private Instant expiredAt; // Thời gian hết hạn thanh toán
    
    @Column(name = "paid_at")
    private Instant paidAt; // Thời gian thanh toán thành công
    
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

