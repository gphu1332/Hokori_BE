package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalRevenueRes {
    private String period; // "2025-01" or "all-time"
    private Long revenueCents;
    private BigDecimal revenue;
    private Integer transactionCount;
    private Integer teacherCount; // Số lượng teachers có doanh thu trong period
    private Integer courseCount; // Số lượng courses có doanh thu trong period
    private List<TransactionDetail> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetail {
        private Long id;
        private Long amountCents;
        private BigDecimal amount;
        private Long teacherId;
        private String teacherName;
        private Long courseId;
        private String courseTitle;
        private String description;
        private Instant createdAt;
    }
}

