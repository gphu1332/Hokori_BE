package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * User's unified request pool for all AI services
 * Updated when user uses AI services or purchases a package
 */
@Entity
@Table(name = "ai_quotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_quota_user",
                columnNames = {"user_id"}
        ),
        indexes = {
                @Index(name = "idx_ai_quota_user", columnList = "user_id")
        })
@Getter
@Setter
public class AIQuota extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_quota_user"))
    private User user;

    /**
     * Total unified requests allocated to user
     * null = unlimited
     */
    @Column(name = "total_requests")
    private Integer totalRequests;

    /**
     * Number of requests used by user
     */
    @Column(name = "used_requests", nullable = false)
    private Integer usedRequests = 0;

    /**
     * Remaining requests available
     * Calculated as: totalRequests - usedRequests
     * null = unlimited
     */
    @Column(name = "remaining_requests")
    private Integer remainingRequests;

    /**
     * Last reset date (for monthly quotas)
     */
    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    // Legacy columns (deprecated, will be removed in future migration)
    @Column(name = "service_type", nullable = true, length = 20)
    private String serviceType; // Deprecated - kept for migration compatibility

    @Column(name = "remaining_quota")
    private Integer remainingQuota; // Deprecated - kept for migration compatibility

    @Column(name = "total_quota")
    private Integer totalQuota; // Deprecated - kept for migration compatibility

    /**
     * Check if user has requests available
     */
    public boolean hasQuota() {
        return totalRequests == null || remainingRequests == null || remainingRequests > 0;
    }

    /**
     * Use requests (decrement)
     */
    public void useRequests(int amount) {
        if (totalRequests != null && remainingRequests != null) {
            usedRequests = (usedRequests != null ? usedRequests : 0) + amount;
            remainingRequests = Math.max(0, totalRequests - usedRequests);
        }
    }

    /**
     * Initialize quota with total requests
     */
    public void initializeQuota(Integer totalRequests) {
        this.totalRequests = totalRequests;
        this.usedRequests = 0;
        this.remainingRequests = totalRequests;
        this.lastResetAt = Instant.now();
    }
}

