package com.hokori.web.controller;

import com.hokori.web.dto.dashboard.LearnerDashboardSummaryRes;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.LearnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learner/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Learner - Dashboard")
public class LearnerDashboardController {

    private final LearnerDashboardService dashboardService;
    private final CurrentUserService currentUserService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('LEARNER')")
    @Operation(summary = "Lấy thông tin tổng quan dashboard cho learner")
    public LearnerDashboardSummaryRes getSummary() {
        Long userId = currentUserService.getCurrentUserId();
        return dashboardService.getSummary(userId);
    }
}
