package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.dashboard.TeacherDashboardSummaryRes;
import com.hokori.web.service.TeacherDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
