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
    
    private Integer grammarQuota;      // null = unlimited
    private Integer kaiwaQuota;        // null = unlimited
    private Integer pronunQuota;       // null = unlimited
    private Integer conversationQuota; // null = unlimited
    
    private Boolean isActive;
    private Integer displayOrder;
}

