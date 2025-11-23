package com.hokori.web.controller;

import com.hokori.web.dto.comment.CourseCommentDto;
import com.hokori.web.service.CourseCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses-public/{courseId}/comments")
@RequiredArgsConstructor
@Tag(name = "Public - Course Comments")
public class CourseCommentPublicController {

    private final CourseCommentService service;

    @Operation(summary = "Danh sách comment của khoá học (root + replies)")
    @GetMapping
    public Page<CourseCommentDto> list(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listCourseComments(courseId, page, size);
    }
}
