package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.TeacherApprovalSubmitRequest;
import com.hokori.web.dto.TeacherProfileDTO;
import com.hokori.web.dto.TeacherQualificationUpdateRequest;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.TeacherProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Các API dành cho giáo viên (Teacher) quản lý hồ sơ và gửi yêu cầu duyệt.
 */
@RestController
@RequestMapping("/api/teachers")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Teacher", description = "Teacher self-service: profile, qualifications, approval flow")
public class TeacherController {

    private final TeacherProfileService service;
    private final CurrentUserService currentUser;

    public TeacherController(TeacherProfileService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/me/profile")
    @Operation(
            summary = "Lấy hồ sơ giáo viên của chính mình",
            description = "Trả về thông tin TeacherProfile đã chuẩn hoá dưới dạng DTO. Yêu cầu đã đăng nhập với vai trò TEACHER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(   // <-- dùng fully-qualified
                    responseCode = "200", description = "Lấy hồ sơ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực / token không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ giáo viên")
    })
    public ResponseEntity<ApiResponse<TeacherProfileDTO>> getMyProfile() {
        Long uid = currentUser.getCurrentUser().orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyProfile(uid)));
    }

    @PutMapping("/me/qualifications")
    @Operation(
            summary = "Cập nhật bằng cấp/chứng chỉ & thông tin chuyên môn",
            description = "Nếu hồ sơ đang REJECTED và có chỉnh sửa, hệ thống sẽ đưa về DRAFT để có thể gửi duyệt lại."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Cập nhật thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền")
    })
    public ResponseEntity<ApiResponse<TeacherProfileDTO>> updateQualifications(
            @Valid @RequestBody TeacherQualificationUpdateRequest req) {
        Long uid = currentUser.getCurrentUser().orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateQualifications(uid, req)));
    }

    @PostMapping("/me/approval")
    @Operation(
            summary = "Gửi yêu cầu duyệt hồ sơ giáo viên",
            description = "Chỉ cho phép khi hồ sơ đang ở DRAFT/REJECTED và thoả điều kiện tối thiểu (bio ≥ 50, headline có nội dung, có bằng cấp/chứng chỉ và bằng chứng)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Gửi yêu cầu thành công (trả về requestId)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Không thoả điều kiện gửi duyệt / dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền (không phải TEACHER)")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitApproval(
            @Valid @RequestBody TeacherApprovalSubmitRequest req) {
        Long uid = currentUser.getCurrentUser().orElseThrow().getId();
        Long requestId = service.submitApproval(uid, req);
        return ResponseEntity.ok(ApiResponse.success("Submitted", Map.of("requestId", requestId)));
    }

    @GetMapping("/me/can-create-course")
    @Operation(
            summary = "Kiểm tra quyền tạo khoá học",
            description = "Trả về { allowed: true|false }. Chỉ khi TeacherProfile ở trạng thái APPROVED mới cho phép tạo khoá học."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Trả trạng thái cho phép tạo khoá học",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> canCreateCourse() {
        Long uid = currentUser.getCurrentUser().orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("allowed", service.canCreateCourse(uid))));
    }
}
