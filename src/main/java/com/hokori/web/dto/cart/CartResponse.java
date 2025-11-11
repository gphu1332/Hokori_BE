package com.hokori.web.dto.cart;

import java.util.List;

import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        long selectedSubtotal
) {}
