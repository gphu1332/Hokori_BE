// com.hokori.web.controller.CourseReviewController.java
package com.hokori.web.controller;

import com.hokori.web.dto.course.CourseFeedbackReq;
import com.hokori.web.dto.course.CourseFeedbackRes;
import com.hokori.web.dto.course.CourseFeedbackSummaryRes;
import com.hokori.web.entity.User;
import com.hokori.web.service.CourseFeedbackService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Course Feedback", description = "Learner feedback (rating + comment) cho khóa học")
public class CourseReviewController {

    private final CourseFeedbackService courseReviewService;
    private final CurrentUserService currentUserService;

    // ====== Public: list feedback cho course detail ======
    @GetMapping
    @Operation(
            summary = "List feedback của 1 khóa học",
            description = "Trả về danh sách feedback (rating + comment) của learner cho 1 khóa học",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = CourseFeedbackRes.class))
                    )
            }
    )
    public List<CourseFeedbackRes> list(@PathVariable Long courseId) {
        return courseReviewService.listFeedbacksForCourse(courseId);
    }

    // ====== Public: summary (avg + count) ======
    @GetMapping("/summary")
    @Operation(
            summary = "Summary điểm đánh giá của khóa học",
            description = "Trả về rating trung bình và số lượt đánh giá"
    )
    public CourseFeedbackSummaryRes summary(@PathVariable Long courseId) {
        return courseReviewService.getSummary(courseId);
    }

    // ====== Learner: tạo / cập nhật feedback ======
    @PostMapping
    @PreAuthorize("hasRole('LEARNER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Tạo / cập nhật feedback cho 1 khóa học",
            description = """
                    - Nếu learner chưa từng feedback khóa học này -> tạo mới.
                    - Nếu đã có feedback trước đó -> cập nhật rating + comment.
                    Yêu cầu learner đã enroll course.
                    """
    )
    public CourseFeedbackRes upsert(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseFeedbackReq req
    ) {
        User me = currentUserService.getCurrentUserOrThrow();
        return courseReviewService.upsertFeedback(me.getId(), courseId, req);
    }

    // ====== Learner: xoá feedback của chính mình ======
    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasRole('LEARNER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Xoá feedback của learner cho 1 khóa học",
            description = "Chỉ owner của feedback mới xoá được; xoá mềm (deleted_flag=true)"
    )
    public void delete(
            @PathVariable Long courseId,
            @PathVariable Long feedbackId
    ) {
        User me = currentUserService.getCurrentUserOrThrow();
        courseReviewService.deleteFeedback(me.getId(), feedbackId);
    }
}
