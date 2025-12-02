package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.dto.moderator.CourseAICheckResponse;
import com.hokori.web.service.CourseService;
import com.hokori.web.service.CourseModerationAIService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moderator Course Approval APIs
 * Chỉ MODERATOR mới có quyền approve/reject courses
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/moderator/courses", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("hasRole('MODERATOR')")
@Tag(name = "Moderator - Course Approval", description = "Duyệt khóa học: approve/reject courses")
@SecurityRequirement(name = "Bearer Authentication")
public class ModeratorCourseController {

    private final CourseService courseService;
    private final CourseModerationAIService moderationAIService;
    private final CurrentUserService currentUserService;

    private Long currentModeratorId() {
        return currentUserService.getCurrentUserId();
    }

    @Operation(
            summary = "Danh sách courses đang chờ duyệt",
            description = "Lấy tất cả courses có status PENDING_APPROVAL"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {"status":"success","message":"OK","data":[
                        {"id":1,"title":"Khóa học N5","status":"PENDING_APPROVAL","userId":5}
                    ]}
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not MODERATOR")
    })
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<CourseRes>>> listPendingApproval() {
        List<CourseRes> courses = courseService.listPendingApprovalCourses();
        return ResponseEntity.ok(ApiResponse.success("OK", courses));
    }

    @Operation(
            summary = "Chi tiết course đang chờ duyệt (FULL TREE)",
            description = "Xem toàn bộ nội dung course (chapters -> lessons -> sections -> contents) để review trước khi approve/reject"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course detail with full tree",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Course is not pending approval"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not MODERATOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<CourseRes>> getDetail(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.getPendingApprovalTree(id);
        return ResponseEntity.ok(ApiResponse.success("OK", course));
    }

    @Operation(
            summary = "Approve course (publish)",
            description = "Duyệt và publish course. Chuyển status từ PENDING_APPROVAL sang PUBLISHED."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course approved and published",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {"status":"success","message":"Course approved","data":{"id":1,"title":"Khóa học N5","status":"PUBLISHED"}}
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Course not in PENDING_APPROVAL status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not MODERATOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CourseRes>> approve(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.approveCourse(id, currentModeratorId());
        return ResponseEntity.ok(ApiResponse.success("Course approved", course));
    }

    @Operation(
            summary = "Reject course",
            description = "Từ chối course. Chuyển status từ PENDING_APPROVAL về DRAFT."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course rejected",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {"status":"success","message":"Course rejected","data":{"id":1,"title":"Khóa học N5","status":"DRAFT"}}
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Course not in PENDING_APPROVAL status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not MODERATOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CourseRes>> reject(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id,
            @Parameter(name = "reason", in = ParameterIn.QUERY, description = "Lý do từ chối (optional)", example = "Thiếu nội dung")
            @RequestParam(required = false) String reason) {
        CourseRes course = courseService.rejectCourse(id, currentModeratorId(), reason);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("course", course);
        if (reason != null && !reason.isBlank()) {
            responseData.put("reason", reason);
        }
        return ResponseEntity.ok(ApiResponse.success("Course rejected", course));
    }

    @Operation(
            summary = "AI Check course content",
            description = "Sử dụng AI để kiểm tra nội dung khóa học: safety check (toxic content), level match. Chỉ áp dụng cho courses có status PENDING_APPROVAL."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI check completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Course not in PENDING_APPROVAL status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not MODERATOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    @GetMapping("/{id}/ai-check")
    public ResponseEntity<ApiResponse<CourseAICheckResponse>> aiCheck(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseAICheckResponse result = moderationAIService.checkCourseContent(id);
        return ResponseEntity.ok(ApiResponse.success("AI check completed", result));
    }
}

