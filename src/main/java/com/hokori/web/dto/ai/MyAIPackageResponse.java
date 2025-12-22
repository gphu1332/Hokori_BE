package com.hokori.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for user's current AI package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAIPackageResponse {
    
    private Boolean hasPackage;
    
    private Long packageId;
    private String packageName;
    
    private Instant purchasedAt;
    private Instant expiresAt;
    
    private String paymentStatus;  // PENDING, PAID, FAILED, etc.
    
    private Boolean isActive;
    private Boolean isExpired;
    
    // Quota information (to help FE determine if user can use AI features)
    private Integer totalRequests;
    private Integer usedRequests;
    private Integer remainingRequests;
    private Boolean hasQuota;  // Whether user has available quota to use AI features
    
    // Package type information
    private Boolean isFreeTier;  // true if user is using free tier (50 requests/month), false if paid package
    private String packageType;  // "FREE_TIER" or "PAID_PACKAGE" or null
}

