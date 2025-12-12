package com.hokori.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for user's AI quotas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQuotaResponse {
    
    /**
     * Map of service type to quota info
     * Key: GRAMMAR, KAIWA, PRONUN, CONVERSATION
     * Value: QuotaInfo
     */
    private Map<String, QuotaInfo> quotas;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaInfo {
        private Integer remainingQuota;  // null = unlimited
        private Integer totalQuota;     // null = unlimited
        private Boolean hasQuota;       // true if can use
    }
}

