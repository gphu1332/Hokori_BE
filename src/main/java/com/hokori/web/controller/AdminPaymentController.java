package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.revenue.*;
import com.hokori.web.service.AdminPaymentService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Tag(name = "Admin Payment Management", description = "APIs để admin quản lý thanh toán cho teachers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {
    
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final AdminPaymentService paymentService;
    private final CurrentUserService currentUserService;
    
    private Long me() {
        return currentUserService.getCurrentUserOrThrow().getId();
    }
    
    @Operation(summary = "Lấy danh sách teachers có revenue chưa được chuyển tiền",
               description = "Group by teacher và yearMonth. Default: tháng hiện tại")
    @GetMapping("/pending-payouts")
    public ResponseEntity<ApiResponse<List<AdminPendingPayoutRes>>> getPendingPayouts(
            @RequestParam(required = false) String yearMonth) {
        
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        List<AdminPendingPayoutRes> payouts = paymentService.getPendingPayouts(yearMonth);
        return ResponseEntity.ok(ApiResponse.success("OK", payouts));
    }
    
    @Operation(summary = "Lấy chi tiết revenue chưa được chuyển tiền của một teacher",
               description = "Xem chi tiết các courses và số tiền cần chuyển")
    @GetMapping("/teacher/{teacherId}/pending-details")
    public ResponseEntity<ApiResponse<AdminPendingPayoutRes>> getTeacherPendingPayoutDetails(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String yearMonth) {
        
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        AdminPendingPayoutRes details = paymentService.getTeacherPendingPayoutDetails(teacherId, yearMonth);
        return ResponseEntity.ok(ApiResponse.success("OK", details));
    }
    
    @Operation(summary = "Đánh dấu revenue đã được chuyển tiền",
               description = "Có thể đánh dấu theo revenueIds hoặc teacherId + yearMonth")
    @PostMapping("/mark-paid")
    public ResponseEntity<ApiResponse<Void>> markPayoutAsPaid(
            @Valid @RequestBody MarkPayoutPaidReq req) {
        paymentService.markPayoutAsPaid(req, me());
        return ResponseEntity.ok(ApiResponse.success("Revenue marked as paid successfully", null));
    }
    
    @Operation(summary = "Tính tổng admin commission trong tháng",
               description = "Tổng 20% commission từ tất cả courses trong tháng")
    @GetMapping("/admin-commission")
    public ResponseEntity<ApiResponse<Long>> getAdminCommission(
            @RequestParam(required = false) String yearMonth) {
        
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        Long commission = paymentService.getAdminCommissionForMonth(yearMonth);
        return ResponseEntity.ok(ApiResponse.success("OK", commission));
    }
}

