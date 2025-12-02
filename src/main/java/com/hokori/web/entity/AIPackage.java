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
     * Quota for each service type (stored as JSON or separate table)
     * For simplicity, we'll use separate columns for each service
     */
    @Column(name = "grammar_quota")
    private Integer grammarQuota;  // null = unlimited

    @Column(name = "kaiwa_quota")
    private Integer kaiwaQuota;  // null = unlimited

    @Column(name = "pronun_quota")
    private Integer pronunQuota;  // null = unlimited

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "aiPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AIPackagePurchase> purchases = new ArrayList<>();
}

