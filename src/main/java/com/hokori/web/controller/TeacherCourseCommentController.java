package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.comment.CommentCreateReq;
import com.hokori.web.dto.comment.CommentUpdateReq;
import com.hokori.web.dto.comment.CourseCommentDto;
import com.hokori.web.service.CourseCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('TEACHER')")
@RestController
@RequestMapping("/api/teacher/courses/{courseId}/comments")
@RequiredArgsConstructor
@Tag(name = "Teacher - Course Comments")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherCourseCommentController {

    private final CourseCommentService service;

    @Operation(summary = "Teacher tạo comment (root) cho khoá học của mình")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseCommentDto>> createRoot(
            @PathVariable Long courseId,
            @Valid @RequestBody CommentCreateReq req
    ) {
        CourseCommentDto dto = service.createRootCommentAsTeacher(courseId, req);
        return ResponseEntity.ok(ApiResponse.success("Created", dto));
    }

    @Operation(summary = "Teacher reply vào 1 comment trên khoá học của mình")
    @PostMapping("/{parentId}/reply")
    public ResponseEntity<ApiResponse<CourseCommentDto>> reply(
            @PathVariable Long courseId,
            @PathVariable Long parentId,
            @Valid @RequestBody CommentCreateReq req
    ) {
        CourseCommentDto dto = service.replyToCommentAsTeacher(courseId, parentId, req);
        return ResponseEntity.ok(ApiResponse.success("Created", dto));
    }

    @Operation(summary = "Teacher chỉnh sửa comment (của mình hoặc moderate)")
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CourseCommentDto>> update(
            @PathVariable Long courseId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateReq req
    ) {
        CourseCommentDto dto = service.updateComment(courseId, commentId, req);
        return ResponseEntity.ok(ApiResponse.success("Updated", dto));
    }

    @Operation(summary = "Teacher xoá (soft delete) comment trên khoá học của mình")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long courseId,
            @PathVariable Long commentId
    ) {
        service.deleteComment(courseId, commentId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
