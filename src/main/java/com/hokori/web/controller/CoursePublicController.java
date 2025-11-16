package com.hokori.web.controller;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.service.CourseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

// Swagger
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Public - Courses", description = "Danh sách & cấu trúc khoá học đã publish (marketplace)")
@SecurityRequirements
public class CoursePublicController {

    private final CourseService service;

    @Operation(summary = "Danh sách khoá học PUBLISHED")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CourseRes.class)))
    @GetMapping
    public Page<CourseRes> list(@RequestParam(required = false) JLPTLevel level,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return service.listPublished(level, page, size);
    }

    @Operation(summary = "Full tree của khoá học (PUBLISHED-only)")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CourseRes.class)))
    @GetMapping("/{id}/tree")
    public CourseRes tree(@PathVariable Long id) {
        return service.getPublishedTree(id);
    }
}
