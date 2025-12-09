package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.dashboard.TeacherDashboardSummaryRes;
import com.hokori.web.dto.revenue.CourseRevenueRes;
import com.hokori.web.dto.revenue.TeacherRevenueRes;
import com.hokori.web.service.TeacherDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('TEACHER')")
@RestController
@RequestMapping("/api/teacher/dashboard")
@RequiredArgsConstructor
@Tag(name = "Teacher - Dashboard")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherDashboardController {

    private final TeacherDashboardService service;

    @Operation(summary = "Tổng quan dashboard cho Teacher")
    @GetMapping
    public ApiResponse<TeacherDashboardSummaryRes> getOverview() {
        return ApiResponse.success("OK", service.getOverview());
    }

    @Operation(
            summary = "Xem revenue theo tháng cụ thể",
            description = "Teacher xem revenue của mình theo tháng. Nếu không có year/month → dùng tháng hiện tại."
    )
    @GetMapping("/revenue")
    public ApiResponse<TeacherRevenueRes> getRevenueByMonth(
            @Parameter(description = "Năm (ví dụ: 2025). Nếu không có → dùng năm hiện tại")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tháng (1-12). Nếu không có → dùng tháng hiện tại")
            @RequestParam(required = false) Integer month
    ) {
        return ApiResponse.success("OK", service.getRevenueByMonth(year, month));
    }

    @Operation(
            summary = "Xem revenue từ một course cụ thể trong tháng",
            description = "Teacher xem revenue từ một course của mình trong tháng. Nếu không có year/month → dùng tháng hiện tại."
    )
    @GetMapping("/courses/{courseId}/revenue")
    public ApiResponse<CourseRevenueRes> getCourseRevenue(
            @Parameter(description = "ID của course")
            @PathVariable Long courseId,
            @Parameter(description = "Năm (ví dụ: 2025). Nếu không có → dùng năm hiện tại")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tháng (1-12). Nếu không có → dùng tháng hiện tại")
            @RequestParam(required = false) Integer month
    ) {
        return ApiResponse.success("OK", service.getCourseRevenue(courseId, year, month));
    }
}
