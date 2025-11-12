package com.hokori.web.controller;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.course.*;
import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Swagger / OpenAPI
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/teacher/courses")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Teacher - Courses", description = "CRUD & cấu trúc khoá học cho giáo viên (ROLE_TEACHER)")
public class TeacherCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    /** Lấy userId (teacher) từ SecurityContext (principal=email). */
    private Long currentUserIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        String email = String.valueOf(auth.getPrincipal());
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }
        return u.getId();
    }

    // ===== Course =====

    @Operation(
            summary = "Tạo khoá học (CREATE)",
            description = "Slug sinh từ title (duy nhất). Mặc định DRAFT; level null => N5.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = CourseRes.class)))
            })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes create(@Valid @RequestBody CourseUpsertReq req) {
        return courseService.createCourse(currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Cập nhật khoá học (UPDATE)",
            description = "Chỉ owner. Nếu đổi title thì cập nhật slug duy nhất.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes update(@PathVariable Long id, @Valid @RequestBody CourseUpsertReq req) {
        return courseService.updateCourse(id, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá mềm khoá học", description = "Đặt deleted_flag=true (không xoá cứng).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.softDelete(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Publish khoá học",
            description = """
            Validate cấu trúc trước khi PUBLISHED:
            - VOCABULARY: phải có flashcardSetId
            - GRAMMAR: đúng 1 nội dung primary (video)
            - KANJI: >= 1 nội dung primary
            """)
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes publish(@PathVariable Long id) {
        return courseService.publish(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Unpublish khoá học", description = "Chuyển về DRAFT.")
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes unpublish(@PathVariable Long id) {
        return courseService.unpublish(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Danh sách khoá học của tôi",
            description = "Phân trang + tìm theo `title/slug` + lọc trạng thái.")
    @Parameters({
            @Parameter(name = "page", description = "Trang (0-based)", example = "0"),
            @Parameter(name = "size", description = "Số phần tử/trang", example = "20"),
            @Parameter(name = "q", description = "Từ khoá (title/slug, không phân biệt hoa thường)"),
            @Parameter(name = "status", description = "DRAFT | PUBLISHED | ARCHIVED",
                    schema = @Schema(implementation = CourseStatus.class))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public Page<CourseRes> listMine(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) CourseStatus status) {
        return courseService.listMine(currentUserIdOrThrow(), page, size, q, status);
    }

    @Operation(summary = "Lấy full cây cấu trúc khoá học",
            description = "Course -> Chapters -> Lessons -> Sections -> Contents (dùng cho màn soạn).")
    @GetMapping("/{id}/tree")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes tree(@PathVariable Long id) {
        return courseService.getTree(id);
    }

    // ===== Children =====

    @Operation(summary = "Thêm Chapter vào Course",
            description = "Nếu orderIndex null: append (đặt = số chapter hiện tại). `isTrial=true` chỉ được 1 chapter.")
    @PostMapping("/{courseId}/chapters")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ChapterRes addChapter(@PathVariable Long courseId,
                                 @Valid @RequestBody ChapterUpsertReq req) {
        return courseService.createChapter(courseId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Thêm Lesson vào Chapter", description = "Nếu orderIndex null: append.")
    @PostMapping("/chapters/{chapterId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public LessonRes addLesson(@PathVariable Long chapterId,
                               @Valid @RequestBody LessonUpsertReq req) {
        return courseService.createLesson(chapterId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Thêm Section vào Lesson",
            description = "VOCABULARY: require flashcardSetId; GRAMMAR/KANJI: có thể null.")
    @PostMapping("/lessons/{lessonId}/sections")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public SectionRes addSection(@PathVariable Long lessonId,
                                 @Valid @RequestBody SectionUpsertReq req) {
        return courseService.createSection(lessonId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Thêm Content vào Section",
            description = """
            - ASSET: require assetId; GRAMMAR chỉ có 1 primaryContent=true
            - RICH_TEXT: require richText; primaryContent=false
            - FLASHCARD_SET: chỉ cho VOCAB; require flashcardSetId
            - QUIZ_REF: require quizId
            """)
    @PostMapping("/sections/{sectionId}/contents")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ContentRes addContent(@PathVariable Long sectionId,
                                 @Valid @RequestBody ContentUpsertReq req) {
        return courseService.createContent(sectionId, currentUserIdOrThrow(), req);
    }
    

    // ===== Course detail (metadata) =====
    @Operation(summary = "Chi tiết khoá học (metadata)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public CourseRes detail(@PathVariable Long id) {
        return courseService.getDetail(id, currentUserIdOrThrow());
    }

    // ====== DTO nhỏ cho PATCH reorder ======
    public record ReorderReq(Integer orderIndex) {}

    // ===== Chapter: update / delete / reorder =====
    @Operation(summary = "Cập nhật Chapter")
    @PutMapping("/chapters/{chapterId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ChapterRes updateChapter(@PathVariable Long chapterId,
                                    @Valid @RequestBody ChapterUpsertReq req) {
        return courseService.updateChapter(chapterId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Chapter", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public void deleteChapter(@PathVariable Long chapterId) {
        courseService.deleteChapter(chapterId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Chapter")
    @PatchMapping("/chapters/{chapterId}/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ChapterRes reorderChapter(@PathVariable Long chapterId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderChapter(chapterId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Lesson: update / delete / reorder =====
    @Operation(summary = "Cập nhật Lesson")
    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public LessonRes updateLesson(@PathVariable Long lessonId,
                                  @Valid @RequestBody LessonUpsertReq req) {
        return courseService.updateLesson(lessonId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Lesson", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public void deleteLesson(@PathVariable Long lessonId) {
        courseService.deleteLesson(lessonId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Lesson")
    @PatchMapping("/lessons/{lessonId}/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public LessonRes reorderLesson(@PathVariable Long lessonId,
                                   @RequestBody ReorderReq req) {
        return courseService.reorderLesson(lessonId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Section: update / delete / reorder =====
    @Operation(summary = "Cập nhật Section")
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public SectionRes updateSection(@PathVariable Long sectionId,
                                    @Valid @RequestBody SectionUpsertReq req) {
        return courseService.updateSection(sectionId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Section", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public void deleteSection(@PathVariable Long sectionId) {
        courseService.deleteSection(sectionId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Section")
    @PatchMapping("/sections/{sectionId}/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public SectionRes reorderSection(@PathVariable Long sectionId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderSection(sectionId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Content: update / delete / reorder =====
    @Operation(summary = "Cập nhật Content trong Section")
    @PutMapping("/sections/contents/{contentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ContentRes updateContent(@PathVariable Long contentId,
                                    @Valid @RequestBody ContentUpsertReq req) {
        return courseService.updateContent(contentId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Content", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/sections/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public void deleteContent(@PathVariable Long contentId) {
        courseService.deleteContent(contentId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Content trong Section")
    @PatchMapping("/sections/contents/{contentId}/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
    public ContentRes reorderContent(@PathVariable Long contentId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderContent(contentId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

}
