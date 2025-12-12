package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Response DTO cho revenue của một course trong tháng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRevenueRes {
    private Long courseId;
    private String courseTitle;
    private Long revenueCents; // 80% của course price
    private Long paidRevenueCents; // Revenue đã được trả tiền
    private Long unpaidRevenueCents; // Revenue chưa được trả tiền
    private Integer salesCount; // Số lượng bán được
    private Integer paidSalesCount; // Số lượng sales đã được trả tiền
    private Integer unpaidSalesCount; // Số lượng sales chưa được trả tiền
    private Boolean isFullyPaid; // Tất cả revenue của course này đã được trả chưa
    private Instant lastPayoutDate; // Ngày trả tiền gần nhất cho course này
    private String payoutStatus; // "FULLY_PAID" | "PARTIALLY_PAID" | "PENDING"
}
