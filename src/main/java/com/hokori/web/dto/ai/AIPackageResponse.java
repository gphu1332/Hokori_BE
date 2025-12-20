package com.hokori.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AI Package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPackageResponse {
    
    private Long id;
    private String name;
    private String description;
    private Long priceCents;
    private String currency;
    private Integer durationDays;
    
    /**
     * Total unified requests in package
     * null = unlimited
     */
    private Integer totalRequests;
    
    // Legacy fields (deprecated)
    private Integer grammarQuota;      // Deprecated
    private Integer kaiwaQuota;        // Deprecated
    private Integer pronunQuota;       // Deprecated
    private Integer conversationQuota; // Deprecated
    
    private Boolean isActive;
    private Integer displayOrder;
}

