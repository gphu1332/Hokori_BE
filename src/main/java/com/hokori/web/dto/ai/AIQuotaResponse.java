package com.hokori.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user's unified AI request pool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQuotaResponse {
    
    /**
     * Total unified requests allocated to user
     * null = unlimited
     */
    private Integer totalRequests;
    
    /**
     * Number of requests used by user
     */
    private Integer usedRequests;
    
    /**
     * Remaining requests available
     * null = unlimited
     */
    private Integer remainingRequests;
    
    /**
     * Whether user has quota available
     */
    private Boolean hasQuota;
}

