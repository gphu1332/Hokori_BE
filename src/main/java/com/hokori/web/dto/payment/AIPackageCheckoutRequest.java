package com.hokori.web.dto.payment;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for checking out an AI Package
 */
public record AIPackageCheckoutRequest(
    @NotNull(message = "Package ID is required")
    Long packageId
) {}

