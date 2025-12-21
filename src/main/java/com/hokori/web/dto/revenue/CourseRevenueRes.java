package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho revenue của một course trong tháng
 * Hỗ trợ cả 2 use cases:
 * 1. Revenue tracking với payout status (new)
 * 2. Dashboard với transaction details (old)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRevenueRes {
    // For revenue tracking (new)
    private Long courseId;
    private String courseTitle;
    private Long originalCoursePriceCents; // Giá gốc khóa học (tổng tiền từ payment cho course này)
    private Long adminCommissionCents; // 20% commission của admin
    private Long revenueCents; // 80% của course price (tiền teacher)
    private Long paidRevenueCents; // Revenue đã được trả tiền
    private Long unpaidRevenueCents; // Revenue chưa được trả tiền
    private Integer salesCount; // Số lượng bán được
    private Integer paidSalesCount; // Số lượng sales đã được trả tiền
    private Integer unpaidSalesCount; // Số lượng sales chưa được trả tiền
    private Boolean isFullyPaid; // Tất cả revenue của course này đã được trả chưa
    private Instant lastPayoutDate; // Ngày trả tiền gần nhất cho course này
    private String payoutStatus; // "FULLY_PAID" | "PARTIALLY_PAID" | "PENDING"
    
    // For backward compatibility (old dashboard)
    private Boolean isPaid; // Deprecated: use isFullyPaid instead
    private Long teacherId;
    private String teacherName;
    private String period; // Format: "2025-01"
    private BigDecimal revenue; // Total revenue as BigDecimal
    private Integer transactionCount;
    private List<TransactionDetail> transactions;
    
    /**
     * Inner class for transaction details (used in dashboard)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetail {
        private Long id;
        private Long amountCents;
        private BigDecimal amount;
        private String description;
        private Instant createdAt;
    }
}
