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
public class CourseRevenueRes {
    private Long teacherId;
    private String teacherName;
    private Long courseId;
    private String courseTitle;
    private String period; // "2025-01"
    private Long revenueCents;
    private BigDecimal revenue;
    private Integer transactionCount;
    private List<TransactionDetail> transactions;

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

