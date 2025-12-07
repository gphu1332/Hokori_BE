package com.hokori.web.dto.payment;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for checking out from cart
 */
public record CheckoutRequest(
    @NotNull(message = "Cart ID is required")
    Long cartId,
    
    List<Long> courseIds // Optional: specific courses to checkout, if null then checkout all selected items
) {}

