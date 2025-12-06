package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.policy.PolicyRes;
import com.hokori.web.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/policies")
@RequiredArgsConstructor
@Tag(name = "Public - Policy", description = "Xem chính sách (policies) theo role - Public access")
@CrossOrigin(origins = "*")
public class PublicPolicyController {

    private final PolicyService policyService;

    @Operation(
            summary = "Lấy policies theo role (Public)",
            description = "Lấy tất cả policies của một role. Endpoint này public, không cần authentication. " +
                    "Dùng để hiển thị chính sách cho user trên frontend."
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
            summary = "Lấy policy theo ID (Public)",
            description = "Lấy chi tiết một policy cụ thể. Endpoint này public, không cần authentication."
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

