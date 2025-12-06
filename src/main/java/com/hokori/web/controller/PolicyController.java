package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.policy.PolicyCreateReq;
import com.hokori.web.dto.policy.PolicyRes;
import com.hokori.web.dto.policy.PolicyUpdateReq;
import com.hokori.web.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Policy Management", description = "Quản lý chính sách (policies) cho từng role")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(
            summary = "Tạo policy mới cho một role",
            description = "Admin tạo chính sách mới cho một role cụ thể. Mỗi policy sẽ gắn với một role."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyRes>> createPolicy(
            @Valid @RequestBody PolicyCreateReq req
    ) {
        try {
            PolicyRes policy = policyService.createPolicy(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Policy created successfully", policy));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create policy: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Update policy",
            description = "Admin cập nhật thông tin của một policy (title và/hoặc content)."
    )
    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyRes>> updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyUpdateReq req
    ) {
        try {
            PolicyRes policy = policyService.updatePolicy(policyId, req);
            return ResponseEntity.ok(ApiResponse.success("Policy updated successfully", policy));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update policy: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Xóa policy",
            description = "Admin xóa một policy."
    )
    @DeleteMapping("/{policyId}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long policyId) {
        try {
            policyService.deletePolicy(policyId);
            return ResponseEntity.ok(ApiResponse.success("Policy deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete policy: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Lấy tất cả policies",
            description = "Admin xem tất cả policies trong hệ thống."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyRes>>> getAllPolicies() {
        try {
            List<PolicyRes> policies = policyService.getAllPolicies();
            return ResponseEntity.ok(ApiResponse.success("Policies retrieved successfully", policies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve policies: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Lấy policies theo role",
            description = "Admin xem tất cả policies của một role cụ thể."
    )
    @GetMapping("/role/{roleName}")
    public ResponseEntity<ApiResponse<List<PolicyRes>>> getPoliciesByRole(
            @PathVariable String roleName
    ) {
        try {
            List<PolicyRes> policies = policyService.getPoliciesByRole(roleName);
            return ResponseEntity.ok(ApiResponse.success("Policies retrieved successfully", policies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve policies: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Lấy policy theo ID",
            description = "Admin xem chi tiết một policy cụ thể."
    )
    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyRes>> getPolicyById(@PathVariable Long policyId) {
        try {
            PolicyRes policy = policyService.getPolicyById(policyId);
            return ResponseEntity.ok(ApiResponse.success("Policy retrieved successfully", policy));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve policy: " + e.getMessage()));
        }
    }
}

