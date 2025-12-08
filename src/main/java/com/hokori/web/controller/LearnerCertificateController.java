package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.certificate.CourseCompletionCertificateRes;
import com.hokori.web.service.CourseCompletionCertificateService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho learner xem certificates (accomplishments) khi hoàn thành courses
 */
@RestController
@RequestMapping("/api/learner/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LEARNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Learner - Certificates", description = "Xem certificates/accomplishments khi hoàn thành courses")
public class LearnerCertificateController {

    private final CourseCompletionCertificateService certificateService;
    private final CurrentUserService currentUserService;

    @Operation(
            summary = "Danh sách tất cả certificates của learner",
            description = "Lấy danh sách tất cả certificates (accomplishments) khi hoàn thành courses, sắp xếp theo thời gian hoàn thành (mới nhất trước)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseCompletionCertificateRes>>> getMyCertificates() {
        Long userId = currentUserService.getCurrentUserId();
        List<CourseCompletionCertificateRes> certificates = certificateService.getMyCertificates(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", certificates));
    }

    @Operation(
            summary = "Lấy certificate cho một course cụ thể",
            description = "Lấy certificate của learner cho một course. Trả về 404 nếu chưa hoàn thành course này."
    )
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<CourseCompletionCertificateRes>> getCertificateByCourse(
            @Parameter(name = "courseId", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long courseId) {
        Long userId = currentUserService.getCurrentUserId();
        CourseCompletionCertificateRes certificate = certificateService.getCertificateByCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("OK", certificate));
    }

    @Operation(
            summary = "Lấy certificate theo ID",
            description = "Lấy chi tiết một certificate theo ID. Chỉ owner mới xem được."
    )
    @GetMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CourseCompletionCertificateRes>> getCertificateById(
            @Parameter(name = "certificateId", in = ParameterIn.PATH, required = true, description = "Certificate ID", example = "1")
            @PathVariable Long certificateId) {
        Long userId = currentUserService.getCurrentUserId();
        CourseCompletionCertificateRes certificate = certificateService.getCertificateById(certificateId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", certificate));
    }
}

