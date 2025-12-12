package com.hokori.web.controller;

import com.hokori.web.Enum.AIServiceType;
import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.ai.*;
import com.hokori.web.service.AIPackageService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI Package Management APIs
 * For managing AI service packages, purchases, and quotas
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/ai/packages", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "AI Packages", description = "Quản lý gói AI: packages, purchases, quotas")
@SecurityRequirement(name = "Bearer Authentication")
public class AIPackageController {

    private final AIPackageService packageService;
    private final CurrentUserService currentUserService;

    private Long currentUserId() {
        return currentUserService.getCurrentUserId();
    }

    // =========================
    // 1. List AI Packages (Optional)
    // =========================
    
    @Operation(
            summary = "Danh sách gói AI",
            description = "Lấy danh sách tất cả gói AI đang active"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AIPackageResponse>>> listPackages() {
        List<AIPackageResponse> packages = packageService.listPackages();
        return ResponseEntity.ok(ApiResponse.success("OK", packages));
    }

    // =========================
    // 2. Check if user has AI package
    // =========================
    
    @Operation(
            summary = "Kiểm tra user có gói AI không",
            description = "Kiểm tra user hiện tại có gói AI đang active không"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-package")
    public ResponseEntity<ApiResponse<MyAIPackageResponse>> getMyPackage() {
        MyAIPackageResponse response = packageService.getMyPackage(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    // =========================
    // 3. Check quota for each service
    // =========================
    
    @Operation(
            summary = "Kiểm tra quota của từng dịch vụ",
            description = "Lấy quota của user cho từng dịch vụ AI (GRAMMAR, KAIWA, PRONUN, CONVERSATION)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<AIQuotaResponse>> getMyQuota() {
        AIQuotaResponse response = packageService.getMyQuota(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    // =========================
    // 4. Create purchase (without payment)
    // =========================
    
    @Operation(
            summary = "Tạo purchase gói AI mới",
            description = "Tạo purchase gói AI cho user. Payment status sẽ là PENDING (thanh toán sẽ tích hợp sau)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Purchase created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request (e.g., already has active package)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<AIPackagePurchaseResponse>> createPurchase(
            @Valid @RequestBody AIPackagePurchaseRequest request) {
        AIPackagePurchaseResponse response = packageService.createPurchase(currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase created successfully", response));
    }

    // =========================
    // 5. Use AI service (deduct quota)
    // =========================
    
    @Operation(
            summary = "Trừ quota mỗi lần user sử dụng AI",
            description = "Trừ quota khi user sử dụng dịch vụ AI. Service type: GRAMMAR, KAIWA, PRONUN, CONVERSATION"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Quota deducted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - No quota available")
    })
    @PostMapping("/services/{serviceType}/use")
    public ResponseEntity<ApiResponse<String>> useAIService(
            @Parameter(name = "serviceType", in = ParameterIn.PATH, required = true, 
                    description = "Service type: GRAMMAR, KAIWA, PRONUN, CONVERSATION", 
                    example = "GRAMMAR")
            @PathVariable String serviceType,
            @Valid @RequestBody UseAIServiceRequest request) {
        
        try {
            AIServiceType serviceTypeEnum = AIServiceType.valueOf(serviceType.toUpperCase());
            int amount = request.getAmount() != null ? request.getAmount() : 1;
            
            packageService.useAIService(currentUserId(), serviceTypeEnum, amount);
            
            return ResponseEntity.ok(ApiResponse.success("Quota deducted successfully", 
                    "Remaining quota updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid service type: " + serviceType));
        }
    }
}

