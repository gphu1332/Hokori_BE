package com.hokori.web.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

/**
 * Request DTO for checking out an AI Package
 */
@Value
public class AIPackageCheckoutRequest {
    @NotNull(message = "Package ID is required")
    Long packageId;
}

