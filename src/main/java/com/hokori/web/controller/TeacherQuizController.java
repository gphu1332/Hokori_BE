package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.quiz.*;
import com.hokori.web.service.TeacherQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Swagger (chỉ dùng cái cơ bản)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN', 'MODERATOR')")
@RestController
@RequestMapping("/api/teacher/sections/{sectionId}/quizzes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Teacher: Quiz Authoring")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherQuizController {

    private final TeacherQuizService service;
    private final com.hokori.web.repository.SectionRepository sectionRepo;

    /**
     * DEPRECATED: Endpoint cũ dùng lessonId - đã chuyển sang sectionId
     * Endpoint này để hỗ trợ migration và trả về error message rõ ràng
     */
    @Deprecated
    @RequestMapping(value = "/api/teacher/lessons/{lessonId}/quizzes", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<ApiResponse<Object>> deprecatedLessonEndpoint(@PathVariable Long lessonId) {
        // Tìm section đầu tiên của lesson để gợi ý
        var sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId);
        String message = "Endpoint đã thay đổi: Quiz giờ thuộc về Section, không phải Lesson. " +
                "Vui lòng dùng endpoint mới: /api/teacher/sections/{sectionId}/quizzes. ";
        if (!sections.isEmpty()) {
            Long firstSectionId = sections.get(0).getId();
            message += "Lesson này có " + sections.size() + " section(s). Section đầu tiên: " + firstSectionId + 
                    ". Nếu lesson có quiz, quiz sẽ nằm trong một section có studyType=QUIZ.";
        } else {
            message += "Lesson này chưa có section nào. Vui lòng tạo section với studyType=QUIZ trước khi tạo quiz.";
        }
        return ResponseEntity.status(410) // 410 Gone - resource no longer available
                .body(ApiResponse.error(message, null));
    }

    @Operation(
            summary = "Lấy quiz của section",
            description = """
            Trả về quiz hiện có của section.
            404 nếu section chưa có quiz.
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<QuizDto>> getBySection(
            @Parameter(description = "ID Section", example = "101")
            @PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getQuizBySection(sectionId)));
    }

    @Operation(
            summary = "Tạo quiz cho section",
            description = """
            Mỗi section chỉ có 1 quiz; nếu đã có → 409.
            Fields:
            - title, description
            - timeLimitSec: Thời gian làm bài (đơn vị: GIÂY). Ví dụ: 30 phút = 1800 giây. null = không giới hạn thời gian
            - passScorePercent [0..100]: Điểm % tối thiểu để pass quiz
            """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<QuizDto>> create(
            @PathVariable Long sectionId,
            @Valid @RequestBody QuizUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.createQuiz(sectionId, req)));
    }

    @Operation(
            summary = "Cập nhật quiz",
            description = """
            Sửa title/description/timeLimitSec/passScorePercent.
            - timeLimitSec: Thời gian làm bài (đơn vị: GIÂY). Ví dụ: 30 phút = 1800 giây. null = không giới hạn
            - passScorePercent: Điểm % tối thiểu để pass quiz [0..100]
            404 nếu quiz không thuộc sectionId.
            """
    )
    @PutMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizDto>> update(
            @Parameter(description = "ID Section", example = "101") @PathVariable Long sectionId,
            @Parameter(description = "ID Quiz", example = "555")  @PathVariable Long quizId,
            @Valid @RequestBody QuizUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateQuiz(sectionId, quizId, req)));
    }

    @Operation(
            summary = "Danh sách câu hỏi + options",
            description = """
            Trả về các câu hỏi theo orderIndex, kèm options của từng câu.
            """
    )
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionWithOptionsDto>>> listQuestions(
            @PathVariable Long sectionId,
            @PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listQuestions(sectionId, quizId)));
    }

    @Operation(
            summary = "Tạo câu hỏi",
            description = """
            Sau khi tạo câu, gọi API thêm options.
            questionType hiện hỗ trợ: SINGLE_CHOICE.
            """
    )
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<ApiResponse<QuestionWithOptionsDto>> createQuestion(
            @PathVariable Long sectionId,
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.createQuestion(sectionId, quizId, req)));
    }

    @Operation(
            summary = "Cập nhật câu hỏi",
            description = "Sửa nội dung, lời giải thích, loại câu, orderIndex."
    )
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionWithOptionsDto>> updateQuestion(
            @PathVariable Long sectionId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateQuestion(sectionId, questionId, req)));
    }

    @Operation(summary = "Xoá câu hỏi", description = "Cascade xoá options; cập nhật lại tổng số câu.")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long sectionId,
            @PathVariable Long questionId) {
        service.deleteQuestion(sectionId, questionId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @Operation(
            summary = "Thêm nhiều options cho 1 câu hỏi",
            description = """
            Ràng buộc:
            - cần >= 2 options
            - SINGLE_CHOICE: đúng chính xác 1 option
            """
    )
    @PostMapping("/questions/{questionId}/options")
    public ResponseEntity<ApiResponse<List<OptionDto>>> addOptions(
            @PathVariable Long sectionId,
            @PathVariable Long questionId,
            @RequestBody List<@Valid OptionUpsertReq> reqs) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.addOptions(sectionId, questionId, reqs)));
    }

    @Operation(
            summary = "Cập nhật option",
            description = "Có thể đổi đáp án đúng; BE sẽ đảm bảo chỉ có 1 đáp án đúng."
    )
    @PutMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<OptionDto>> updateOption(
            @PathVariable Long sectionId,
            @PathVariable Long optionId,
            @Valid @RequestBody OptionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateOption(sectionId, optionId, req)));
    }

    @Operation(
            summary = "Xoá option",
            description = "Không cho xoá nếu còn < 2 options hoặc đó là đáp án đúng duy nhất."
    )
    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable Long sectionId,
            @PathVariable Long optionId) {
        service.deleteOption(sectionId, optionId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
