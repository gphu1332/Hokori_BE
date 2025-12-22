package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho lịch sử chuyển tiền của admin
 * Group by teacher và payoutDate để hiển thị các lần đã chuyển tiền
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPayoutHistoryRes {
    // Teacher info
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    
    // Payout info
    private Instant payoutDate; // Ngày chuyển tiền
    private Long payoutByUserId; // Admin đã chuyển tiền
    private String payoutByUserName; // Tên admin đã chuyển tiền
    private String payoutNote; // Ghi chú khi chuyển tiền
    
    // Revenue info
    private String yearMonth; // Tháng của revenue (format: "2025-01")
    private Long totalPaidRevenueCents; // Tổng số tiền đã chuyển cho teacher này trong lần này
    private Long totalAdminCommissionCents; // Tổng admin commission từ các courses này
    private Integer totalSales; // Tổng số sales đã được chuyển tiền
    
    // Course details
    private List<CourseRevenueRes> courses; // Danh sách các courses đã được chuyển tiền trong lần này
}

