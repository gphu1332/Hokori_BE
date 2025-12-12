package com.hokori.web.dto.revenue;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * Request DTO để admin đánh dấu revenue đã được chuyển tiền
 */
@Data
public class MarkPayoutPaidReq {
    
    /**
     * Option 1: Đánh dấu theo danh sách revenue IDs cụ thể
     */
    private List<Long> revenueIds;
    
    /**
     * Option 2: Đánh dấu tất cả revenue chưa được chuyển tiền của teacher trong tháng
     */
    private Long teacherId;
    private String yearMonth; // Format: "2025-01"
    
    /**
     * Ghi chú khi chuyển tiền (optional)
     */
    private String note;
}

