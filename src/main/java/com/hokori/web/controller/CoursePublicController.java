package com.hokori.web.controller;

import com.hokori.web.Enum.JLPTLevel;            // Đảm bảo đúng package enum của bạn
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// --- Swagger / OpenAPI ---
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Public API cho marketplace (không cần JWT).
 *
 * Ràng buộc:
 * - Chỉ hiển thị khoá học ở trạng thái PUBLISHED.
 * - Lọc tuỳ chọn theo JLPT level (N5..N1).
 * - Xem chi tiết cấu trúc (/tree) cũng chỉ áp dụng cho PUBLISHED; nếu không, trả 404 để tránh lộ thông tin.
 *
 * Gợi ý FE:
 * - Phân trang: page bắt đầu từ 0, size mặc định 20.
 * - Nên cache ngắn hạn (ETag/If-None-Match hoặc Cache-Control) cho danh sách public.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Public - Courses", description = "Danh sách và cấu trúc khoá học đã publish (marketplace)")
public class CoursePublicController {

    private final CourseService service;

    @Operation(
            summary = "Danh sách khoá học PUBLISHED (marketplace)",
            description = """
            Trả về danh sách khoá học đã publish, phân trang.
            - Lọc tuỳ chọn theo JLPT level (N5..N1).
            - Sắp xếp mặc định theo ngày publish (mới nhất trước).
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Page.class))),
            }
    )
    @GetMapping
    public Page<CourseRes> list(
            @Parameter(description = "JLPT level để lọc (tuỳ chọn)") @RequestParam(required = false) JLPTLevel level,
            @Parameter(description = "Trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size
    ) {
        // Service đã chỉ trả PUBLISHED
        return service.listPublished(level, page, size);
    }

    @Operation(
            summary = "Cấu trúc đầy đủ của khoá học (chỉ khi PUBLISHED)",
            description = """
            Trả về full tree: Course -> Chapters -> Lessons -> Sections -> Contents.
            - Chỉ hiển thị nếu khoá học đang ở trạng thái PUBLISHED.
            - Nếu không phải PUBLISHED, trả 404 (ẩn thông tin khoá học).
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = CourseRes.class))),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy hoặc chưa publish")
            }
    )
    @GetMapping("/{id}/tree")
    public CourseRes tree(@Parameter(description = "ID khoá học") @PathVariable Long id) {
        // Lấy tree (có status trong response)
        CourseRes res = service.getTree(id);
        if (res == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");

        // Chỉ công khai khi PUBLISHED
        if (res.getStatus() == null || !"PUBLISHED".equals(res.getStatus().name())) {
            // 404 để tránh lộ việc khoá học tồn tại nhưng chưa publish
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course is not published");
        }
        return res;
    }
}
