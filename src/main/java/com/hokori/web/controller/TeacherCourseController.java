package com.hokori.web.controller;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.course.*;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.hokori.web.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

// Swagger / OpenAPI
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/courses")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Teacher - Courses", description = "CRUD & cấu trúc khoá học cho giáo viên (ROLE_TEACHER)")
public class TeacherCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /** Lấy userId (teacher) từ SecurityContext (principal=email). */
    private Long currentUserIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        String email = String.valueOf(auth.getPrincipal());
        // Use query that avoids LOB fields to prevent LOB stream errors
        var statusOpt = userRepository.findUserActiveStatusByEmail(email);
        if (statusOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Object[] status = statusOpt.get();
        // Handle nested array case (PostgreSQL)
        Object[] actualStatus = status;
        if (status.length == 1 && status[0] instanceof Object[]) {
            actualStatus = (Object[]) status[0];
        }
        if (actualStatus.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Long userId = ((Number) actualStatus[0]).longValue();
        Object isActiveObj = actualStatus[1];
        boolean isActive = false;
        if (isActiveObj instanceof Boolean) {
            isActive = (Boolean) isActiveObj;
        } else if (isActiveObj instanceof Number) {
            isActive = ((Number) isActiveObj).intValue() != 0;
        } else {
            String isActiveStr = isActiveObj.toString().toLowerCase().trim();
            isActive = "true".equals(isActiveStr) || "1".equals(isActiveStr);
        }
        if (!isActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }
        return userId;
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
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes create(@Valid @RequestBody CourseUpsertReq req) {
        return courseService.createCourse(currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Cập nhật khoá học (UPDATE)",
            description = "Chỉ owner. Nếu đổi title thì cập nhật slug duy nhất.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes update(@PathVariable Long id, @Valid @RequestBody CourseUpsertReq req) {
        return courseService.updateCourse(id, currentUserIdOrThrow(), req);
    }

    @Operation(
            summary = "Xoá mềm khoá học",
            description = "Đặt deleted_flag=true (không xoá cứng)."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        // ID teacher hiện tại
        Long teacherId = currentUserIdOrThrow();

        // Xoá mềm
        courseService.softDelete(id, teacherId);

        // Trả JSON cho FE / Swagger thấy rõ
        Map<String, Object> body = new HashMap<>();
        body.put("courseId", id);
        body.put("message", "Course deleted successfully");

        return ResponseEntity.ok(body); // HTTP 200 + JSON
    }


    @Operation(summary = "Publish khoá học",
            description = """
            Validate cấu trúc trước khi PUBLISHED:
            - VOCABULARY: phải có flashcardSetId
            - GRAMMAR: đúng 1 nội dung primary (video)
            - KANJI: >= 1 nội dung primary
            """)
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes publish(@PathVariable Long id) {
        return courseService.publish(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Unpublish khoá học", description = "Chuyển về DRAFT.")
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes unpublish(@PathVariable Long id) {
        return courseService.unpublish(id, currentUserIdOrThrow());
    }

    @Operation(
            summary = "Upload ảnh cover cho khoá học",
            description = "Nhận file ảnh (multipart/form-data), lưu vào thư mục uploads/courses/{courseId}/cover và cập nhật coverImagePath cho Course."
    )
    @PostMapping(
            value = "/{courseId}/cover-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes uploadCoverImage(@PathVariable Long courseId,
                                      @RequestParam("file") MultipartFile file) {
        Long teacherId = currentUserIdOrThrow();

        // thư mục con: courses/{courseId}/cover
        String subFolder = "courses/" + courseId + "/cover";

        // lưu file và lấy relative path (vd: courses/10/cover/uuid.png)
        String relativePath = fileStorageService.store(file, subFolder);

        // cập nhật coverImagePath trong Course
        return courseService.updateCoverImage(courseId, teacherId, relativePath);
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
    @PreAuthorize("hasRole('TEACHER')")
    public Page<CourseRes> listMine(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) CourseStatus status) {
        return courseService.listMine(currentUserIdOrThrow(), page, size, q, status);
    }

    @Operation(summary = "Lấy full cây cấu trúc khoá học",
            description = "Course -> Chapters -> Lessons -> Sections -> Contents (dùng cho màn soạn).")
    @GetMapping("/{id}/tree")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes tree(@PathVariable Long id) {
        return courseService.getTree(id);
    }

    // ===== Children =====

    @Operation(summary = "Thêm Chapter vào Course",
            description = "Nếu orderIndex null: append (đặt = số chapter hiện tại). `isTrial=true` chỉ được 1 chapter.")
    @PostMapping("/{courseId}/chapters")
    @PreAuthorize("hasRole('TEACHER')")
    public ChapterRes addChapter(@PathVariable Long courseId,
                                 @Valid @RequestBody ChapterUpsertReq req) {
        return courseService.createChapter(courseId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Thêm Lesson vào Chapter", description = "Nếu orderIndex null: append.")
    @PostMapping("/chapters/{chapterId}/lessons")
    @PreAuthorize("hasRole('TEACHER')")
    public LessonRes addLesson(@PathVariable Long chapterId,
                               @Valid @RequestBody LessonUpsertReq req) {
        return courseService.createLesson(chapterId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Thêm Section vào Lesson",
            description = "VOCABULARY: require flashcardSetId; GRAMMAR/KANJI: có thể null.")
    @PostMapping("/lessons/{lessonId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public SectionRes addSection(@PathVariable Long lessonId,
                                 @Valid @RequestBody SectionUpsertReq req) {
        return courseService.createSection(lessonId, currentUserIdOrThrow(), req);
    }

    @Operation(
            summary = "Upload file (video/audio/docx...) cho Section",
            description = """
                Nhận file binary (multipart/form-data), lưu vào thư mục uploads/sections/{sectionId},
                trả về:
                - filePath: đường dẫn tương đối trong hệ thống (dùng để set ContentUpsertReq.filePath)
                - url: URL đầy đủ để FE có thể preview ("/files/" + filePath).
                
                Sau khi upload xong, FE tạo Content bằng API:
                POST /api/teacher/courses/sections/{sectionId}/contents
                với contentFormat = ASSET, filePath = giá trị trả về ở đây.
                """
    )
    @PostMapping(
            value = "/sections/{sectionId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('TEACHER')")
    public FileUploadRes uploadSectionFile(@PathVariable Long sectionId,
                                           @RequestParam("file") MultipartFile file) {
        // ép user phải đăng nhập + active (dùng luôn hàm có sẵn)
        currentUserIdOrThrow();  // TODO: nếu muốn check đúng owner theo section thì đưa logic vào service

        String subFolder = "sections/" + sectionId;
        String relativePath = fileStorageService.store(file, subFolder);

        String url = "/files/" + relativePath;  // FE tự prepend domain nếu cần
        return new FileUploadRes(relativePath, url);
    }



    @Operation(summary = "Thêm Content vào Section",
            description = """
            - ASSET: require filePath (đường dẫn file đã upload); GRAMMAR chỉ có 1 primaryContent=true
            - RICH_TEXT: require richText; primaryContent=false
            - FLASHCARD_SET: chỉ cho VOCAB; require flashcardSetId
            - QUIZ_REF: require quizId
            """)
    @PostMapping("/sections/{sectionId}/contents")
    @PreAuthorize("hasRole('TEACHER')")
    public ContentRes addContent(@PathVariable Long sectionId,
                                 @Valid @RequestBody ContentUpsertReq req) {
        return courseService.createContent(sectionId, currentUserIdOrThrow(), req);
    }
    

    // ===== Course detail (metadata) =====
    @Operation(summary = "Chi tiết khoá học (metadata)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes detail(@PathVariable Long id) {
        return courseService.getDetail(id, currentUserIdOrThrow());
    }

    // ====== DTO nhỏ cho PATCH reorder ======
    public record ReorderReq(Integer orderIndex) {}

    // ====== DTO kết quả upload file ======
    public record FileUploadRes(String filePath, String url) {}

    // ===== Chapter: update / delete / reorder =====
    @Operation(summary = "Cập nhật Chapter")
    @PutMapping("/chapters/{chapterId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ChapterRes updateChapter(@PathVariable Long chapterId,
                                    @Valid @RequestBody ChapterUpsertReq req) {
        return courseService.updateChapter(chapterId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Chapter", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteChapter(@PathVariable Long chapterId) {
        courseService.deleteChapter(chapterId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Chapter")
    @PatchMapping("/chapters/{chapterId}/reorder")
    @PreAuthorize("hasRole('TEACHER')")
    public ChapterRes reorderChapter(@PathVariable Long chapterId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderChapter(chapterId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Lesson: update / delete / reorder =====
    @Operation(summary = "Cập nhật Lesson")
    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public LessonRes updateLesson(@PathVariable Long lessonId,
                                  @Valid @RequestBody LessonUpsertReq req) {
        return courseService.updateLesson(lessonId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Lesson", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteLesson(@PathVariable Long lessonId) {
        courseService.deleteLesson(lessonId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Lesson")
    @PatchMapping("/lessons/{lessonId}/reorder")
    @PreAuthorize("hasRole('TEACHER')")
    public LessonRes reorderLesson(@PathVariable Long lessonId,
                                   @RequestBody ReorderReq req) {
        return courseService.reorderLesson(lessonId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Section: update / delete / reorder =====
    @Operation(summary = "Cập nhật Section")
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public SectionRes updateSection(@PathVariable Long sectionId,
                                    @Valid @RequestBody SectionUpsertReq req) {
        return courseService.updateSection(sectionId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Section", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteSection(@PathVariable Long sectionId) {
        courseService.deleteSection(sectionId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Section")
    @PatchMapping("/sections/{sectionId}/reorder")
    @PreAuthorize("hasRole('TEACHER')")
    public SectionRes reorderSection(@PathVariable Long sectionId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderSection(sectionId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Content: update / delete / reorder =====
    @Operation(summary = "Cập nhật Content trong Section")
    @PutMapping("/sections/contents/{contentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ContentRes updateContent(@PathVariable Long contentId,
                                    @Valid @RequestBody ContentUpsertReq req) {
        return courseService.updateContent(contentId, currentUserIdOrThrow(), req);
    }

    @Operation(summary = "Xoá Content", description = "Xoá cứng; tự chuẩn hoá lại orderIndex.")
    @DeleteMapping("/sections/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteContent(@PathVariable Long contentId) {
        courseService.deleteContent(contentId, currentUserIdOrThrow());
    }

    @Operation(summary = "Đổi thứ tự Content trong Section")
    @PatchMapping("/sections/contents/{contentId}/reorder")
    @PreAuthorize("hasRole('TEACHER')")
    public ContentRes reorderContent(@PathVariable Long contentId,
                                     @RequestBody ReorderReq req) {
        return courseService.reorderContent(contentId, currentUserIdOrThrow(),
                req.orderIndex() == null ? 0 : req.orderIndex());
    }

}
