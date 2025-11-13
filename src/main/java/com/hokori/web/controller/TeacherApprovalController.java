package com.hokori.web.controller;

import com.hokori.web.dto.*;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.TeacherApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('TEACHER')")
@RestController
@RequestMapping("/api/teacher/approval")
@Tag(name = "Teacher: Approval", description = "Quản lý chứng chỉ & gửi yêu cầu duyệt hồ sơ mở bán khóa học (chỉ ROLE_TEACHER).")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer Authentication")
// Dùng FQCN để không cần import, tránh xung đột tên
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class TeacherApprovalController {

    private final CurrentUserService currentUserService;
    private final TeacherApprovalService approvalService;

    private Long meId(){ return currentUserService.getCurrentUserOrThrow().getId(); }

    // ===================== Certificates =====================

    @GetMapping("/certificates")
    @Operation(
            summary = "Danh sách chứng chỉ của tôi",
            description = "Trả về danh sách User_Certificates của current user (teacher).",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "OK",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = UserCertificateDto.class)),
                                    examples = @ExampleObject(
                                            value = """
                                            {
                                              "status":"success",
                                              "message":"OK",
                                              "data":[{ "id":101, "title":"JLPT N2", "...":"..." }]
                                            }
                                            """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    public ResponseEntity<ApiResponse<List<UserCertificateDto>>> listCertificates(){
        return ResponseEntity.ok(ApiResponse.success("OK",
                approvalService.listMyCertificates(meId())));
    }

    @PostMapping("/certificates")
    @Operation(
            summary = "Thêm chứng chỉ",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserCertificateReq.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "title":"TESOL",
                                      "issueDate":"2021-05-10",
                                      "credentialId":"TESOL-2021-XYZ"
                                    }
                                    """
                            ))),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Created",
                            content = @Content(schema = @Schema(implementation = UserCertificateDto.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    public ResponseEntity<ApiResponse<UserCertificateDto>> addCertificate(
            @Valid @RequestBody UserCertificateReq req){
        return ResponseEntity.ok(ApiResponse.success("Created",
                approvalService.addCertificate(meId(), req)));
    }

    @PutMapping("/certificates/{id}")
    @Operation(
            summary = "Cập nhật chứng chỉ",
            parameters = @Parameter(name = "id", description = "ID chứng chỉ", required = true),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserCertificateReq.class))),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Updated",
                            content = @Content(schema = @Schema(implementation = UserCertificateDto.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request / Not owner"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
            }
    )
    public ResponseEntity<ApiResponse<UserCertificateDto>> updateCertificate(
            @PathVariable Long id, @Valid @RequestBody UserCertificateReq req){
        return ResponseEntity.ok(ApiResponse.success("Updated",
                approvalService.updateCertificate(meId(), id, req)));
    }

    @DeleteMapping("/certificates/{id}")
    @Operation(
            summary = "Xoá chứng chỉ",
            parameters = @Parameter(name = "id", description = "ID chứng chỉ", required = true),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Deleted",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"status\":\"success\",\"message\":\"Deleted\",\"data\":null}")))
                    ,
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Not owner"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
            }
    )
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(@PathVariable Long id){
        approvalService.deleteCertificate(meId(), id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ===================== Submit Approval =====================

    @PostMapping("/submit")
    @Operation(
            summary = "Nộp yêu cầu duyệt hồ sơ mở bán",
            description = "Nếu không gửi `certificateIds`, hệ thống sẽ snapshot **tất cả** chứng chỉ hiện có.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(schema = @Schema(implementation = SubmitApprovalReq.class))),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Submitted",
                            content = @Content(schema = @Schema(implementation = ApproveRequestDto.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already PENDING / No certificates"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    public ResponseEntity<ApiResponse<ApproveRequestDto>> submit(
            @RequestBody(required = false) SubmitApprovalReq req){
        return ResponseEntity.ok(ApiResponse.success("Submitted",
                approvalService.submitApproval(meId(), req)));
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Xem yêu cầu duyệt gần nhất",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = ApproveRequestDto.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No request yet")
            }
    )
    public ResponseEntity<ApiResponse<ApproveRequestDto>> latest(){
        return ResponseEntity.ok(ApiResponse.success("OK",
                approvalService.getLatestRequest(meId())));
    }
}
