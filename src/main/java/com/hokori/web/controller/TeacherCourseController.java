package com.hokori.web.controller;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.course.*;
import com.hokori.web.dto.teacher.TeacherLearnerStatisticsResponse;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.CourseService;
import com.hokori.web.service.FileStorageService;
import com.hokori.web.service.TeacherStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

// Swagger
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@Slf4j
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
    private final TeacherStatisticsService teacherStatisticsService;
    private final com.hokori.web.repository.SectionRepository sectionRepo;
    private final com.hokori.web.repository.CourseRepository courseRepo;
    private final com.hokori.web.service.CourseFlagService courseFlagService;

    /** Lấy userId (teacher) từ SecurityContext (principal=email). */
    private Long currentUserIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        String email = String.valueOf(auth.getPrincipal());
        var statusOpt = userRepository.findUserActiveStatusByEmail(email);
        if (statusOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Object[] status = statusOpt.get();
        Object[] actual = (status.length == 1 && status[0] instanceof Object[]) ? (Object[]) status[0] : status;
        if (actual.length < 2) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        Long userId = ((Number) actual[0]).longValue();

        boolean isActive;
        Object activeObj = actual[1];
        if (activeObj instanceof Boolean b) isActive = b;
        else if (activeObj instanceof Number n) isActive = n.intValue() != 0;
        else isActive = "true".equalsIgnoreCase(activeObj.toString()) || "1".equals(activeObj.toString());
        if (!isActive) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");

        return userId;
    }

    // ===== Course =====

    @Operation(summary = "Tạo khoá học (CREATE)",
            description = "Slug sinh từ title (duy nhất). Mặc định DRAFT; level null => N5.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CourseRes.class)))
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

    @Operation(summary = "Xoá mềm khoá học", description = "Đặt deleted_flag=true (không xoá cứng).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        courseService.softDelete(id, currentUserIdOrThrow());
        Map<String, Object> body = new HashMap<>();
        body.put("courseId", id);
        body.put("message", "Course deleted successfully");
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Submit khoá học để duyệt (hoặc resubmit sau khi bị reject)", description = """
        Submit course để moderator duyệt:
        - Có thể submit từ status: DRAFT, REJECTED, hoặc FLAGGED
        - Nếu course bị REJECTED, teacher có thể sửa và gọi lại endpoint này để resubmit
        - Khi resubmit, rejection info sẽ được clear
        - Validate trước khi submit:
          - Teacher phải có approvalStatus = APPROVED
          - Course phải có title
          - Course phải có đúng 1 trial chapter
        - Chuyển status sang PENDING_APPROVAL
        """)
    @PutMapping("/{id}/submit-for-approval")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes submitForApproval(@PathVariable Long id) {
        return courseService.submitForApproval(id, currentUserIdOrThrow());
    }

    /**
     * Backward compatibility endpoint - redirects to submit-for-approval
     * @deprecated Use /{id}/submit-for-approval instead
     */
    @Operation(summary = "[DEPRECATED] Submit khoá học để duyệt", 
               description = "Deprecated: Use /{id}/submit-for-approval instead")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    @Deprecated
    public CourseRes publish(@PathVariable Long id) {
        // Redirect to submit-for-approval for backward compatibility
        return courseService.submitForApproval(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Unpublish khoá học", description = "Chuyển về DRAFT.")
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes unpublish(@PathVariable Long id) {
        return courseService.unpublish(id, currentUserIdOrThrow());
    }

    @Operation(summary = "Upload ảnh cover cho khoá học",
            description = "Multipart file; lưu vào uploads/courses/{courseId}/cover và cập nhật coverImagePath.")
    @PostMapping(value = "/{courseId}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes uploadCoverImage(@PathVariable Long courseId,
                                      @RequestParam("file") MultipartFile file) {
        try {
            Long teacherId = currentUserIdOrThrow();
            log.debug("Uploading cover image for courseId={}, teacherId={}, fileName={}, size={}", 
                courseId, teacherId, file.getOriginalFilename(), file.getSize());
            
            // Validate file
            if (file == null || file.isEmpty()) {
                log.warn("Empty file uploaded for courseId={}", courseId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or null");
            }
            
            String subFolder = "courses/" + courseId + "/cover";
            String relativePath = fileStorageService.store(file, subFolder);
            log.debug("File stored successfully: {}", relativePath);
            
            CourseRes result = courseService.updateCoverImage(courseId, teacherId, relativePath);
            log.debug("Cover image updated successfully for courseId={}", courseId);
            return result;
        } catch (ResponseStatusException e) {
            log.warn("ResponseStatusException when uploading cover image for courseId={}: {}", 
                courseId, e.getReason());
            throw e; // Re-throw ResponseStatusException as-is
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException when uploading cover image for courseId={}: {}", 
                courseId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error when uploading cover image for courseId={}", courseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to upload cover image: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
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

    // ======= ĐỔI TÊN: tree -> detail (FULL TREE) =======
    @Operation(summary = "Chi tiết khoá học (FULL TREE cho màn soạn)",
            description = "Course -> Chapters -> Lessons -> Sections -> Contents")
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseRes detailFullTree(@PathVariable Long id) {
        // chỉ owner mới xem được, check ở service getTree (getOwned trong đó)
        return courseService.getTree(id);
    }

    // ===== Children =====

    @Operation(summary = "Thêm Chapter vào Course",
            description = "Nếu orderIndex null: append; `isTrial=true` chỉ được 1 chapter.")
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

    @Operation(summary = "Upload file cho Section",
            description = """
            Multipart -> uploads/sections/{sectionId}.
            FE nhận về filePath để gọi API tạo Content (ASSET).
            Hỗ trợ video, audio, và các file media khác.
            """)
    @PostMapping(value = "/sections/{sectionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public FileUploadRes uploadSectionFile(@PathVariable Long sectionId,
                                           @RequestParam("file") MultipartFile file) {
        try {
            Long teacherId = currentUserIdOrThrow();
            log.debug("Uploading file for sectionId={}, teacherId={}, fileName={}, size={}", 
                sectionId, teacherId, file.getOriginalFilename(), file.getSize());
            
            // Validate file
            if (file == null || file.isEmpty()) {
                log.warn("Empty file uploaded for sectionId={}", sectionId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or null");
            }
            
            // Check ownership - validate teacher owns the course containing this section
            Long courseId = sectionRepo.findCourseIdBySectionId(sectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            
            // Check if teacher owns the course (same logic as CourseService.assertOwner)
            if (!courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner of this course");
            }
            
            String subFolder = "sections/" + sectionId;
            String relativePath = fileStorageService.store(file, subFolder);
            String url = "/files/" + relativePath;
            
            log.debug("File uploaded successfully: sectionId={}, filePath={}", sectionId, relativePath);
            return new FileUploadRes(relativePath, url);
        } catch (ResponseStatusException e) {
            log.warn("ResponseStatusException when uploading file for sectionId={}: {}", 
                sectionId, e.getReason());
            throw e; // Re-throw ResponseStatusException as-is
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException when uploading file for sectionId={}: {}", 
                sectionId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error when uploading file for sectionId={}", sectionId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to upload file: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Operation(summary = "Thêm Content vào Section",
            description = """
        - ASSET: require filePath; GRAMMAR chỉ 1 primaryContent=true
        - RICH_TEXT: require richText; primaryContent=false
        - FLASHCARD_SET: chỉ cho VOCAB; require flashcardSetId
        """)
    @PostMapping("/sections/{sectionId}/contents")
    @PreAuthorize("hasRole('TEACHER')")
    public ContentRes addContent(@PathVariable Long sectionId,
                                 @Valid @RequestBody ContentUpsertReq req) {
        return courseService.createContent(sectionId, currentUserIdOrThrow(), req);
    }


    // ====== DTO nhỏ ======
    public record ReorderReq(Integer orderIndex) {}
    public record FileUploadRes(String filePath, String url) {}

    // ===== Chapter =====
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
        return courseService.reorderChapter(
                chapterId, currentUserIdOrThrow(), req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Lesson =====
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
        return courseService.reorderLesson(
                lessonId, currentUserIdOrThrow(), req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Section =====
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
        return courseService.reorderSection(
                sectionId, currentUserIdOrThrow(), req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Content =====
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
        return courseService.reorderContent(
                contentId, currentUserIdOrThrow(), req.orderIndex() == null ? 0 : req.orderIndex());
    }

    // ===== Statistics =====

    @Operation(
            summary = "Teacher xem statistics của learners trong course",
            description = """
                    Xem thống kê chi tiết về learners đã enroll vào course:
                    - Statistics tổng hợp: tổng số learners, số đang học, số đã hoàn thành, % progress trung bình
                    - Danh sách từng learner với progress chi tiết
                    
                    Chỉ teacher owner của course mới được xem.
                    """
    )
    @GetMapping("/{courseId}/learners/statistics")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeacherLearnerStatisticsResponse> getLearnerStatistics(@PathVariable Long courseId) {
        return ApiResponse.success("OK", teacherStatisticsService.getCourseLearnerStatistics(courseId));
    }

    @Operation(
            summary = "Xem lý do course bị flag",
            description = "Teacher xem lý do course bị flag và danh sách các flags từ users. Chỉ owner của course mới xem được."
    )
    @GetMapping("/{courseId}/flag-reason")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<com.hokori.web.dto.course.FlaggedCourseRes> getFlagReason(@PathVariable Long courseId) {
        Long teacherId = currentUserIdOrThrow();
        com.hokori.web.dto.course.FlaggedCourseRes flagReason = courseFlagService.getFlagReason(courseId, teacherId);
        return ApiResponse.success("OK", flagReason);
    }

    @Operation(
            summary = "Resubmit course sau khi sửa",
            description = "Teacher resubmit course sau khi đã sửa nội dung theo lý do flag. Chuyển status từ FLAGGED về PENDING_APPROVAL để moderator review lại."
    )
    @PutMapping("/{courseId}/resubmit")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseRes> resubmitCourse(@PathVariable Long courseId) {
        Long teacherId = currentUserIdOrThrow();
        courseFlagService.resubmitCourse(courseId, teacherId);
        CourseRes course = courseService.getTree(courseId);
        return ApiResponse.success("Course resubmitted for review", course);
    }
}
