package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response DTO cho admin xem danh sách teacher cần chuyển tiền
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPendingPayoutRes {
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    
    // Bank account info
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
    private String bankBranchName;
    
    // Revenue summary
    private String yearMonth; // Format: "2025-01"
    private Long totalPendingRevenueCents; // Tổng tiền cần chuyển trong tháng này
    private Integer totalPendingSales; // Số lượng sales chưa được chuyển tiền
    
    // Chi tiết các courses
    private List<CourseRevenueRes> courses;
}

