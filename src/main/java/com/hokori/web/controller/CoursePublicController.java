package com.hokori.web.controller;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.dto.course.LessonRes;
import com.hokori.web.dto.progress.ContentProgressRes;
import com.hokori.web.dto.flashcard.FlashcardSetResponse;
import com.hokori.web.dto.flashcard.FlashcardResponse;
import com.hokori.web.service.CourseService;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.LearnerProgressService;
import com.hokori.web.service.CourseFlagService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

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
    private final CurrentUserService currentUserService;
    private final LearnerProgressService progressService;
    private final CourseFlagService courseFlagService;

    @Operation(summary = "Danh sách khoá học PUBLISHED")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CourseRes.class)))
    @GetMapping
    public Page<CourseRes> list(@RequestParam(required = false) JLPTLevel level,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        // Get userId if authenticated (optional)
        Long userId = currentUserService.getUserIdOrNull();
        return service.listPublished(level, page, size, userId);
    }

    @Operation(
            summary = "Full tree của khoá học (PUBLISHED-only)",
            description = "Lấy cấu trúc đầy đủ của course (chapters -> lessons -> sections -> contents). " +
                    "Public endpoint, không cần enrollment. Nếu user đã đăng nhập, sẽ trả về isEnrolled để FE biết đã enroll chưa."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CourseRes.class)))
    @GetMapping("/{id}/tree")
    public CourseRes tree(@PathVariable Long id) {
        // Get userId if authenticated (optional)
        Long userId = currentUserService.getUserIdOrNull();
        return service.getPublishedTree(id, userId);
    }

    @Operation(
            summary = "Trial tree của khoá học (chỉ chapter học thử)",
            description = "Lấy cấu trúc chỉ trial chapter của course PUBLISHED. Không cần enrollment để xem. Guest có thể xem."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CourseRes.class)))
    @GetMapping("/{id}/trial-tree")
    public CourseRes trialTree(@PathVariable Long id) {
        return service.getTrialTree(id);
    }

    @Operation(
            summary = "Chi tiết trial lesson (public - guest có thể xem)",
            description = "Xem lesson detail với sections và contents của trial chapter. Không cần authentication hoặc enrollment."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = LessonRes.class)))
    @GetMapping("/lessons/{lessonId}/trial-detail")
    public LessonRes getTrialLessonDetail(@PathVariable Long lessonId) {
        return progressService.getTrialLessonDetail(lessonId);
    }

    @Operation(
            summary = "Danh sách contents của trial lesson (public - guest có thể xem)",
            description = "Lấy danh sách contents của trial lesson. Không cần authentication hoặc enrollment. Không track progress."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = ContentProgressRes.class)))
    @GetMapping("/lessons/{lessonId}/trial-contents")
    public List<ContentProgressRes> getTrialLessonContents(@PathVariable Long lessonId) {
        return progressService.getTrialLessonContents(lessonId);
    }

    @Operation(
            summary = "Lấy flashcard set cho trial content (public - guest có thể xem)",
            description = "Lấy flashcard set gắn với section content trong trial chapter. Không cần authentication hoặc enrollment."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = com.hokori.web.dto.flashcard.FlashcardSetResponse.class)))
    @GetMapping("/contents/{sectionContentId}/trial-flashcard")
    public FlashcardSetResponse getTrialFlashcardSet(@PathVariable Long sectionContentId) {
        return progressService.getTrialFlashcardSetForContent(sectionContentId);
    }

    @Operation(
            summary = "Lấy danh sách flashcard cards cho trial content (public - guest có thể xem)",
            description = "Lấy danh sách flashcard cards trong flashcard set gắn với section content trong trial chapter. Không cần authentication hoặc enrollment."
    )
    @ApiResponse(responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FlashcardResponse.class))))
    @GetMapping("/contents/{sectionContentId}/trial-flashcard/cards")
    public List<FlashcardResponse> getTrialFlashcardCards(@PathVariable Long sectionContentId) {
        return progressService.getTrialFlashcardCards(sectionContentId);
    }

    @Operation(
            summary = "Flag/Report course",
            description = "User flag một course đã PUBLISHED. Chỉ cho phép flag course đã publish và user chưa flag course này trước đó. Yêu cầu authentication."
    )
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{courseId}/flag")
    public ResponseEntity<Map<String, String>> flagCourse(
            @PathVariable Long courseId,
            @RequestBody com.hokori.web.dto.course.CourseFlagReq req) {
        Long userId = currentUserService.getUserIdOrThrow();
        courseFlagService.flagCourse(courseId, userId, req);
        return ResponseEntity.ok(Map.of("message", "Course flagged successfully"));
    }
}
