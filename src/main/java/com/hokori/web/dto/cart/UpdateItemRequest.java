package com.hokori.web.dto.cart;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;

public record UpdateItemRequest(
        @Nullable @Min(1) Integer quantity,
        @Nullable Boolean selected
) {}
