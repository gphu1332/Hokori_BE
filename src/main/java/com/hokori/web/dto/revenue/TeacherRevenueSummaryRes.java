package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho tổng hợp revenue của teacher theo tháng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRevenueSummaryRes {
    private String yearMonth; // Format: "2025-01"
    private Long totalRevenueCents; // Tổng revenue của tháng (80% từ tất cả courses)
    private Long unpaidRevenueCents; // Revenue chưa được chuyển tiền
    private Long paidRevenueCents; // Revenue đã được chuyển tiền
    private Integer totalSales; // Số lượng bán được trong tháng
    private Integer paidSales; // Số lượng sales đã được trả tiền
    private Integer unpaidSales; // Số lượng sales chưa được trả tiền
    
    // Payout status info
    private Boolean isFullyPaid; // Tất cả revenue trong tháng đã được trả chưa
    private Instant lastPayoutDate; // Ngày trả tiền gần nhất trong tháng này
    private String payoutStatus; // "FULLY_PAID" | "PARTIALLY_PAID" | "PENDING"
    
    private List<CourseRevenueRes> courses; // Revenue theo từng course
}

