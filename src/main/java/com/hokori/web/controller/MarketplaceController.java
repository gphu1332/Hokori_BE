package com.hokori.web.controller;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.service.CourseService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

// Swagger
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Marketplace API - Courses marketplace với field isEnrolled
 * Endpoint này trả về danh sách courses với field isEnrolled để FE có thể filter
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Marketplace - Danh sách khóa học với thông tin enrollment")
@SecurityRequirements
public class MarketplaceController {

    private final CourseService courseService;
    private final CurrentUserService currentUserService;

    @Operation(
            summary = "Danh sách khóa học PUBLISHED (Marketplace)",
            description = "Trả về danh sách khóa học đã publish với field isEnrolled. " +
                    "isEnrolled = true nếu user đã enroll, false nếu chưa enroll, null nếu chưa đăng nhập."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Danh sách khóa học",
            content = @Content(schema = @Schema(implementation = CourseRes.class))
    )
    @GetMapping("/courses")
    public Page<CourseRes> listCourses(
            @RequestParam(required = false) JLPTLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Get userId if authenticated (optional - null if not logged in)
        Long userId = currentUserService.getUserIdOrNull();
        return courseService.listPublished(level, page, size, userId);
    }
}

