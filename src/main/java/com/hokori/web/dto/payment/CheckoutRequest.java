package com.hokori.web.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class CheckoutRequest {
    @NotNull(message = "Cart ID is required")
    Long cartId;
    
    List<Long> courseIds; // Optional: specific courses to checkout, if null then checkout all selected items
}

