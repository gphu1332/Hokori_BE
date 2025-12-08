package com.hokori.web.dto.cart;

import java.util.List;

/**
 * Simplified cart info for checkout - chỉ chứa thông tin cần thiết để thanh toán
 */
public record CartCheckoutInfo(
        Long cartId,
        List<Long> courseIds  // Danh sách courseIds đã được chọn (selected = true)
) {}

