package com.hokori.web.dto.cart;

/**
 * Response DTO for cart item with course information for display
 */
public record CartItemResponse(
        Long itemId,
        Long courseId,
        int quantity,
        long totalPrice,
        boolean selected,
        // Course information for display
        String courseTitle,
        String courseSlug,
        String coverImagePath,
        String teacherName
) {}
