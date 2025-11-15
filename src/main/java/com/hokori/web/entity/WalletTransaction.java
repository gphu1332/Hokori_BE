package com.hokori.web.entity;

import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_tx_user", columnList = "user_id"),
                @Index(name = "idx_wallet_tx_course", columnList = "course_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chủ ví bị ảnh hưởng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_wallet_tx_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private WalletTransactionStatus status;

    // Số tiền thay đổi (đơn vị cent), dương = cộng, âm = trừ
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    // Số dư sau khi áp dụng giao dịch này
    @Column(name = "balance_after_cents", nullable = false)
    private Long balanceAfterCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50, nullable = false)
    private WalletTransactionSource source;

    // Khóa học liên quan (COURSE_SALE), có thể null với payout / điều chỉnh
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_wallet_tx_course")
    )
    private Course course;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Id user tạo giao dịch (admin / system), có thể null
    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
