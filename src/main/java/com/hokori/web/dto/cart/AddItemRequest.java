package com.hokori.web.dto.cart;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(
        @NotNull Long courseId,
        @Min(1) Integer quantity
) {}
