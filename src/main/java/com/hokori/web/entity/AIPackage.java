package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Package definition (e.g., Basic, Premium, Pro)
 */
@Entity
@Table(name = "ai_packages")
@Getter
@Setter
public class AIPackage extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;  // e.g., "Basic", "Premium", "Pro"

    @Column(length = 500)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;  // Price in cents (VND)

    @Column(name = "currency", length = 10)
    private String currency = "VND";

    /**
     * Duration in days (e.g., 30 = 1 month, 90 = 3 months)
     */
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    /**
     * Total unified requests in package
     * null = unlimited
     */
    @Column(name = "total_requests")
    private Integer totalRequests;

    // Legacy columns (deprecated, will be removed in future migration)
    @Column(name = "grammar_quota")
    private Integer grammarQuota;  // Deprecated - kept for migration compatibility

    @Column(name = "kaiwa_quota")
    private Integer kaiwaQuota;  // Deprecated - kept for migration compatibility

    @Column(name = "pronun_quota")
    private Integer pronunQuota;  // Deprecated - kept for migration compatibility

    @Column(name = "conversation_quota")
    private Integer conversationQuota;  // Deprecated - kept for migration compatibility

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "aiPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AIPackagePurchase> purchases = new ArrayList<>();
}

