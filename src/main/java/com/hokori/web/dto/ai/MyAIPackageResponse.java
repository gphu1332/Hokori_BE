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
}

