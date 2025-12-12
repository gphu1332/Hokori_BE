package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho revenue của teacher
 * Hỗ trợ cả 2 use cases:
 * 1. Revenue tracking với payout status (new)
 * 2. Dashboard với transaction details (old)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRevenueRes {
    // For revenue tracking (new)
    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long paymentId;
    private Long enrollmentId;
    
    // Revenue amounts (in cents)
    private Long totalAmountCents;
    private Long coursePriceCents;
    private Long teacherRevenueCents; // 80%
    private Long adminCommissionCents; // 20%
    
    // Time info
    private String yearMonth; // Format: "2025-01"
    private Instant paidAt;
    
    // Payout info
    private Boolean isPaid;
    private Instant payoutDate;
    private Long payoutByUserId;
    private String payoutNote;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    // For dashboard (old - backward compatibility)
    private String period; // Format: "2025-01" (same as yearMonth)
    private Long revenueCents; // Total revenue in cents
    private BigDecimal revenue; // Total revenue as BigDecimal
    private Integer transactionCount;
    private Long walletBalance;
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
        private Long courseId;
        private String courseTitle;
        private String description;
        private Instant createdAt;
    }
}
