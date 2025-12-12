package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.revenue.*;
import com.hokori.web.service.TeacherRevenueService;
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
@RequestMapping("/api/teacher/revenue")
@RequiredArgsConstructor
@Tag(name = "Teacher Revenue", description = "APIs để teacher quản lý revenue và bank account")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherRevenueController {
    
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final TeacherRevenueService revenueService;
    private final CurrentUserService currentUserService;
    
    private Long me() {
        return currentUserService.getCurrentUserOrThrow().getId();
    }
    
    @Operation(summary = "Cập nhật thông tin tài khoản ngân hàng", 
               description = "Chỉ được phép khi teacher đã được APPROVED")
    @PutMapping("/bank-account")
    public ResponseEntity<ApiResponse<Void>> updateBankAccount(
            @Valid @RequestBody TeacherBankAccountUpdateReq req) {
        revenueService.updateBankAccount(me(), req);
        return ResponseEntity.ok(ApiResponse.success("Bank account updated successfully", null));
    }
    
    @Operation(summary = "Lấy trạng thái thanh toán trong tháng",
               description = "Hiển thị trạng thái đã trả/chưa trả tiền của teacher trong tháng")
    @GetMapping("/payout-status")
    public ResponseEntity<ApiResponse<PayoutStatusRes>> getPayoutStatus(
            @RequestParam(required = false) String yearMonth) {
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        PayoutStatusRes status = revenueService.getPayoutStatus(me(), yearMonth);
        return ResponseEntity.ok(ApiResponse.success("OK", status));
    }
    
    @Operation(summary = "Lấy tổng hợp revenue theo tháng với filter",
               description = "Filter theo isPaid (true/false/null) và courseId")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TeacherRevenueSummaryRes>> getRevenueSummary(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) Long courseId) {
        
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        TeacherRevenueFilterReq filter = new TeacherRevenueFilterReq();
        filter.setYearMonth(yearMonth);
        filter.setIsPaid(isPaid);
        filter.setCourseId(courseId);
        
        TeacherRevenueSummaryRes summary = revenueService.getRevenueSummary(me(), filter);
        return ResponseEntity.ok(ApiResponse.success("OK", summary));
    }
    
    @Operation(summary = "Lấy danh sách revenue tất cả các tháng")
    @GetMapping("/all-summaries")
    public ResponseEntity<ApiResponse<List<TeacherRevenueSummaryRes>>> getAllRevenueSummaries() {
        List<TeacherRevenueSummaryRes> summaries = revenueService.getAllRevenueSummaries(me());
        return ResponseEntity.ok(ApiResponse.success("OK", summaries));
    }
    
    @Operation(summary = "Lấy chi tiết revenue của một course trong tháng")
    @GetMapping("/course/{courseId}/details")
    public ResponseEntity<ApiResponse<List<TeacherRevenueRes>>> getCourseRevenueDetails(
            @PathVariable Long courseId,
            @RequestParam(required = false) String yearMonth) {
        
        // Default to current month if not provided
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        List<TeacherRevenueRes> details = revenueService.getCourseRevenueDetails(me(), courseId, yearMonth);
        return ResponseEntity.ok(ApiResponse.success("OK", details));
    }
}

