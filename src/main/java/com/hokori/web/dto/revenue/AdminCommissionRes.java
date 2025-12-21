package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho admin commission trong tháng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommissionRes {
    private String yearMonth; // Format: "2025-01"
    
    /**
     * Doanh thu dự kiến: Tổng 20% commission từ các revenue chưa được trả tiền (isPaid = false)
     * Đây là số tiền admin sẽ nhận được khi chưa xác nhận chuyển tiền cho teachers
     */
    private Long expectedRevenueCents; // Doanh thu dự kiến (chưa trả tiền)
    
    /**
     * Doanh thu tháng này khi đã chuyển tiền: Tổng 20% commission từ các revenue đã được trả tiền (isPaid = true)
     * Đây là số tiền admin đã nhận được sau khi đã xác nhận chuyển tiền cho teachers
     */
    private Long paidRevenueCents; // Doanh thu đã chuyển tiền
    
    /**
     * Tổng doanh thu (expected + paid)
     */
    private Long totalRevenueCents; // Tổng doanh thu
}

