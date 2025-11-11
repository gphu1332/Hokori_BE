package com.hokori.web.dto.cart;

public record CartItemResponse(
        Long itemId,
        Long courseId,
        int quantity,
        long totalPrice,
        boolean selected
) {}
