package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.ai.*;
import com.hokori.web.service.AIPackageService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Admin Controller for AI Package Management
 * Only ADMIN can access these endpoints
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/admin/ai-packages", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - AI Package Management", description = "Quản lý gói AI: tạo, sửa, xóa packages")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*")
public class AdminAIPackageController {

    private final AIPackageService packageService;

    @Operation(
            summary = "Tạo gói AI mới",
            description = "Admin tạo gói AI mới (Plus, Pro, etc.)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AIPackageResponse>> createPackage(
            @Valid @RequestBody AIPackageCreateReq request) {
        AIPackageResponse response = packageService.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("AI Package created successfully", response));
    }

    @Operation(
            summary = "Cập nhật gói AI",
            description = "Admin cập nhật thông tin gói AI"
    )
    @PutMapping("/{packageId}")
    public ResponseEntity<ApiResponse<AIPackageResponse>> updatePackage(
            @PathVariable Long packageId,
            @Valid @RequestBody AIPackageUpdateReq request) {
        AIPackageResponse response = packageService.updatePackage(packageId, request);
        return ResponseEntity.ok(ApiResponse.success("AI Package updated successfully", response));
    }

    @Operation(
            summary = "Xóa gói AI",
            description = "Admin xóa gói AI (chỉ được xóa nếu không có active purchases)"
    )
    @DeleteMapping("/{packageId}")
    public ResponseEntity<ApiResponse<String>> deletePackage(@PathVariable Long packageId) {
        packageService.deletePackage(packageId);
        return ResponseEntity.ok(ApiResponse.success("AI Package deleted successfully", null));
    }

    @Operation(
            summary = "Lấy tất cả gói AI",
            description = "Admin lấy danh sách tất cả gói AI (bao gồm cả inactive)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<AIPackageResponse>>> getAllPackages() {
        List<AIPackageResponse> packages = packageService.getAllPackages();
        return ResponseEntity.ok(ApiResponse.success("OK", packages));
    }

    @Operation(
            summary = "Lấy gói AI theo ID",
            description = "Admin lấy thông tin chi tiết của một gói AI"
    )
    @GetMapping("/{packageId}")
    public ResponseEntity<ApiResponse<AIPackageResponse>> getPackageById(@PathVariable Long packageId) {
        AIPackageResponse response = packageService.getPackageById(packageId);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }
}

