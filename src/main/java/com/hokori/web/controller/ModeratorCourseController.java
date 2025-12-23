package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.dto.moderator.CourseAICheckResponse;
import com.hokori.web.service.CourseService;
import com.hokori.web.service.CourseModerationAIService;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.CourseCommentService;
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
    private final com.hokori.web.service.CourseFlagService courseFlagService;
    private final CourseCommentService commentService;

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
            description = "Từ chối course. Chuyển status từ PENDING_APPROVAL về REJECTED. Teacher có thể sửa và submit lại sau."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course rejected",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {"status":"success","message":"Course rejected","data":{"id":1,"title":"Khóa học N5","status":"REJECTED"}}
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

    @Operation(
            summary = "Danh sách courses bị flag",
            description = "Lấy danh sách courses bị flag, sắp xếp theo số lượng flag (cao đến thấp). Chỉ hiển thị courses có status PUBLISHED hoặc FLAGGED."
    )
    @GetMapping("/flagged")
    public ResponseEntity<ApiResponse<List<com.hokori.web.dto.course.FlaggedCourseRes>>> listFlaggedCourses() {
        List<com.hokori.web.dto.course.FlaggedCourseRes> flaggedCourses = courseFlagService.listFlaggedCourses();
        return ResponseEntity.ok(ApiResponse.success("OK", flaggedCourses));
    }

    @Operation(
            summary = "Flag course (unpublish)",
            description = "Moderator flag course và set status = FLAGGED. Course sẽ bị ẩn khỏi public. Tổng hợp lý do từ các flags của users."
    )
    @PutMapping("/{id}/flag")
    public ResponseEntity<ApiResponse<CourseRes>> flagCourse(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        courseFlagService.moderatorFlagCourse(id, currentModeratorId());
        CourseRes course = courseService.getTree(id);
        return ResponseEntity.ok(ApiResponse.success("Course flagged", course));
    }

    @Operation(
            summary = "Danh sách courses có update đang chờ duyệt",
            description = "Lấy tất cả courses có status PENDING_UPDATE (teacher đã submit update từ PUBLISHED course)"
    )
    @GetMapping("/pending-updates")
    public ResponseEntity<ApiResponse<List<CourseRes>>> listPendingUpdates() {
        List<CourseRes> courses = courseService.listPendingUpdateCourses();
        return ResponseEntity.ok(ApiResponse.success("OK", courses));
    }
    
    @Operation(
            summary = "Disable comments cho course",
            description = "Moderator tắt chức năng comment cho course. Dùng khi course có nhiều spam hoặc toxic comments."
    )
    @PutMapping("/{id}/disable-comments")
    public ResponseEntity<ApiResponse<CourseRes>> disableComments(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.disableComments(id, currentModeratorId());
        return ResponseEntity.ok(ApiResponse.success("Comments disabled for this course", course));
    }
    
    @Operation(
            summary = "Enable comments cho course",
            description = "Moderator bật lại chức năng comment cho course đã bị disable."
    )
    @PutMapping("/{id}/enable-comments")
    public ResponseEntity<ApiResponse<CourseRes>> enableComments(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.enableComments(id, currentModeratorId());
        return ResponseEntity.ok(ApiResponse.success("Comments enabled for this course", course));
    }
    
    @Operation(
            summary = "Ẩn một comment cụ thể",
            description = "Moderator ẩn (disable) một comment của user bằng cách đánh dấu status là Disabled. Comment sẽ không hiển thị cho learners. Dùng khi comment có nội dung spam, toxic, hoặc vi phạm quy định."
    )
    @PutMapping("/{courseId}/comments/{commentId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableComment(
            @Parameter(name = "courseId", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long courseId,
            @Parameter(name = "commentId", in = ParameterIn.PATH, required = true, description = "Comment ID", example = "1")
            @PathVariable Long commentId) {
        commentService.disableCommentAsModerator(courseId, commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment đã được ẩn", null));
    }
    
    @Operation(
            summary = "Hiện lại một comment đã bị ẩn",
            description = "Moderator khôi phục (restore/enable) một comment đã bị ẩn. Comment sẽ hiển thị lại cho learners. Dùng khi moderator ẩn nhầm hoặc sau khi review lại."
    )
    @PutMapping("/{courseId}/comments/{commentId}/restore")
    public ResponseEntity<ApiResponse<com.hokori.web.dto.comment.CourseCommentDto>> restoreComment(
            @Parameter(name = "courseId", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long courseId,
            @Parameter(name = "commentId", in = ParameterIn.PATH, required = true, description = "Comment ID", example = "1")
            @PathVariable Long commentId) {
        com.hokori.web.dto.comment.CourseCommentDto comment = commentService.restoreCommentAsModerator(courseId, commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment đã được hiện lại", comment));
    }

    @Operation(
            summary = "Chi tiết course có update đang chờ (FULL TREE)",
            description = "Xem toàn bộ nội dung course (chapters -> lessons -> sections -> contents) để review update trước khi approve/reject"
    )
    @GetMapping("/{id}/update-detail")
    public ResponseEntity<ApiResponse<CourseRes>> getUpdateDetail(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.getTree(id);
        // Verify it's actually PENDING_UPDATE
        if (course.getStatus() != com.hokori.web.Enum.CourseStatus.PENDING_UPDATE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Course is not in PENDING_UPDATE status"));
        }
        return ResponseEntity.ok(ApiResponse.success("OK", course));
    }

    @Operation(
            summary = "Approve course update",
            description = "Duyệt và áp dụng update cho course. Chuyển status từ PENDING_UPDATE về PUBLISHED và clear pendingUpdateAt."
    )
    @PutMapping("/{id}/approve-update")
    public ResponseEntity<ApiResponse<CourseRes>> approveUpdate(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id) {
        CourseRes course = courseService.approveUpdate(id, currentModeratorId());
        return ResponseEntity.ok(ApiResponse.success("Course update approved", course));
    }

    @Operation(
            summary = "Reject course update",
            description = "Từ chối update. Revert course về PUBLISHED status và clear pendingUpdateAt. Teacher có thể sửa và submit update lại sau."
    )
    @PutMapping("/{id}/reject-update")
    public ResponseEntity<ApiResponse<CourseRes>> rejectUpdate(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Course ID", example = "1")
            @PathVariable Long id,
            @Parameter(name = "reason", in = ParameterIn.QUERY, description = "Lý do từ chối (optional)", example = "Nội dung không phù hợp")
            @RequestParam(required = false) String reason) {
        CourseRes course = courseService.rejectUpdate(id, currentModeratorId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Course update rejected", course));
    }
}

