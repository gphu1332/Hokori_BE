package com.hokori.web.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Response DTO cho trạng thái thanh toán của teacher trong tháng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutStatusRes {
    private String yearMonth; // Format: "2025-01"
    
    // Tổng quan
    private Long totalRevenueCents; // Tổng revenue tháng này
    private Long paidRevenueCents; // Đã được trả tiền
    private Long unpaidRevenueCents; // Chưa được trả tiền
    
    // Trạng thái
    private String payoutStatus; // "FULLY_PAID" | "PARTIALLY_PAID" | "PENDING"
    private Boolean isFullyPaid; // Tất cả đã được trả chưa
    private Instant lastPayoutDate; // Ngày trả tiền gần nhất
    private String lastPayoutNote; // Ghi chú khi trả tiền gần nhất
    
    // Thống kê
    private Integer totalSales; // Tổng số sales
    private Integer paidSales; // Số sales đã được trả tiền
    private Integer unpaidSales; // Số sales chưa được trả tiền
    
    // Thông tin bank account (để teacher biết tiền sẽ chuyển vào đâu)
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
    private String bankBranchName;
}

