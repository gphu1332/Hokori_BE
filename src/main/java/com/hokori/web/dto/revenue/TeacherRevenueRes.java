package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Response DTO cho revenue của teacher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRevenueRes {
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
}
