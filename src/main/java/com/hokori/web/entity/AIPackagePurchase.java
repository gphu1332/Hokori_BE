package com.hokori.web.entity;

import com.hokori.web.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * User's purchase of an AI package
 */
@Entity
@Table(name = "ai_package_purchases",
        indexes = {
                @Index(name = "idx_ai_purchase_user", columnList = "user_id"),
                @Index(name = "idx_ai_purchase_status", columnList = "payment_status"),
                @Index(name = "idx_ai_purchase_active", columnList = "user_id,is_active")
        })
@Getter
@Setter
public class AIPackagePurchase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_purchase_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_purchase_package"))
    private AIPackage aiPackage;

    @Column(name = "purchase_price_cents", nullable = false)
    private Long purchasePriceCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Only one active purchase per user at a time
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    /**
     * Payment transaction ID (if integrated with payment gateway)
     */
    @Column(name = "transaction_id", length = 255)
    private String transactionId;
}

