package com.hokori.web.entity;

import com.hokori.web.Enum.AIServiceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * User's quota for each AI service type
 * Updated when user uses AI services or purchases a package
 */
@Entity
@Table(name = "ai_quotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_service",
                columnNames = {"user_id", "service_type"}
        ),
        indexes = {
                @Index(name = "idx_ai_quota_user", columnList = "user_id"),
                @Index(name = "idx_ai_quota_service", columnList = "service_type")
        })
@Getter
@Setter
public class AIQuota extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_quota_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private AIServiceType serviceType;

    /**
     * Current remaining quota
     * null = unlimited
     */
    @Column(name = "remaining_quota")
    private Integer remainingQuota;

    /**
     * Total quota allocated (for tracking)
     * null = unlimited
     */
    @Column(name = "total_quota")
    private Integer totalQuota;

    /**
     * Last reset date (for monthly quotas)
     */
    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    /**
     * Check if user has quota available
     */
    public boolean hasQuota() {
        return remainingQuota == null || remainingQuota > 0;
    }

    /**
     * Use quota (decrement)
     */
    public void useQuota(int amount) {
        if (remainingQuota != null) {
            remainingQuota = Math.max(0, remainingQuota - amount);
        }
    }
}

