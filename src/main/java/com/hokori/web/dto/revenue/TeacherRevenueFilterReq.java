package com.hokori.web.dto.revenue;

import lombok.Data;

/**
 * Request DTO để filter revenue của teacher
 */
@Data
public class TeacherRevenueFilterReq {
    /**
     * Filter theo tháng (Format: "2025-01")
     * Nếu null → lấy tháng hiện tại
     */
    private String yearMonth;
    
    /**
     * Filter theo trạng thái thanh toán
     * null = tất cả
     * true = chỉ lấy đã được trả tiền
     * false = chỉ lấy chưa được trả tiền
     */
    private Boolean isPaid;
    
    /**
     * Filter theo course ID
     * null = tất cả courses
     */
    private Long courseId;
}

