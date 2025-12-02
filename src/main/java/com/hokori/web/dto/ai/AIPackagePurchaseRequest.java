package com.hokori.web.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for purchasing an AI package
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIPackagePurchaseRequest {
    
    @NotNull(message = "Package ID is required")
    private Long packageId;
    
    // Note: Payment integration will be added later
    // For now, paymentStatus will be set to PENDING
}

