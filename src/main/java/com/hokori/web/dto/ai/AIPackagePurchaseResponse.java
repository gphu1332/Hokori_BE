package com.hokori.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for AI Package Purchase
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPackagePurchaseResponse {
    
    private Long id;
    private Long packageId;
    private String packageName;
    private Long purchasePriceCents;
    private String paymentStatus;
    private Instant purchasedAt;
    private Instant expiresAt;
    private Boolean isActive;
    private String transactionId;
}

