package com.hokori.web.controller;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.ApproveDecisionReq;
import com.hokori.web.dto.ApproveRequestDto;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.TeacherApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teacher-approval")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Admin - Teacher approval",
        description = "Admin/Staff duyệt hồ sơ mở bán khóa học cho Teacher")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherApprovalAdminController {

    private final TeacherApprovalService approvalService;
    private final CurrentUserService currentUserService;

    private Long meId() {
        return currentUserService.getCurrentUserOrThrow().getId();
    }

    // 1) List request theo status (mặc định PENDING)
    @GetMapping("/requests")
    @Operation(
            summary = "Danh sách yêu cầu duyệt hồ sơ",
            description = "status = PENDING / APPROVED / REJECTED (optional, default PENDING)"
    )
    public ResponseEntity<ApiResponse<List<ApproveRequestDto>>> listRequests(
            @RequestParam(name = "status", required = false) ApprovalStatus status) {

        List<ApproveRequestDto> data = approvalService.listRequests(status);
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    // 2) Xem chi tiết 1 request (bao gồm toàn bộ certificates snapshot)
    @GetMapping("/requests/{id}")
    @Operation(summary = "Xem chi tiết yêu cầu duyệt hồ sơ")
    public ResponseEntity<ApiResponse<ApproveRequestDto>> getRequest(@PathVariable Long id) {
        ApproveRequestDto dto = approvalService.getRequest(id);
        return ResponseEntity.ok(ApiResponse.success("OK", dto));
    }

    // 3) Quyết định APPROVED / REJECTED cho 1 request
    @PostMapping("/requests/{id}/decision")
    @Operation(
            summary = "Admin quyết định APPROVED / REJECTED",
            description = "Body chứa action = APPROVED hoặc REJECTED và note (optional)"
    )
    public ResponseEntity<ApiResponse<ApproveRequestDto>> decide(
            @PathVariable Long id,
            @Valid @RequestBody ApproveDecisionReq req) {

        Long adminUserId = meId();
        ApproveRequestDto dto = approvalService.adminDecide(id, adminUserId, req);
        return ResponseEntity.ok(ApiResponse.success("Decided", dto));
    }
}
