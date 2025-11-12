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

@PreAuthorize("hasAnyRole('TEACHER', 'STAFF', 'ADMIN')")
@RestController
@RequestMapping("/api/teacher/lessons/{lessonId}/quizzes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Teacher: Quiz Authoring")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherQuizController {

    private final TeacherQuizService service;

    @Operation(
            summary = "Lấy quiz của lesson",
            description = """
            Trả về quiz hiện có của lesson.
            404 nếu lesson chưa có quiz.
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<QuizDto>> getByLesson(
            @Parameter(description = "ID Lesson", example = "101")
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getQuizByLesson(lessonId)));
    }

    @Operation(
            summary = "Tạo quiz cho lesson",
            description = """
            Mỗi lesson chỉ có 1 quiz; nếu đã có → 409.
            Fields:
            - title, description
            - timeLimitSec (null = không giới hạn)
            - passScorePercent [0..100]
            """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<QuizDto>> create(
            @PathVariable Long lessonId,
            @Valid @RequestBody QuizUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.createQuiz(lessonId, req)));
    }

    @Operation(
            summary = "Cập nhật quiz",
            description = """
            Sửa title/description/timeLimitSec/passScorePercent.
            404 nếu quiz không thuộc lessonId.
            """
    )
    @PutMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizDto>> update(
            @Parameter(description = "ID Lesson", example = "101") @PathVariable Long lessonId,
            @Parameter(description = "ID Quiz", example = "555")  @PathVariable Long quizId,
            @Valid @RequestBody QuizUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateQuiz(lessonId, quizId, req)));
    }

    @Operation(
            summary = "Danh sách câu hỏi + options",
            description = """
            Trả về các câu hỏi theo orderIndex, kèm options của từng câu.
            """
    )
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionWithOptionsDto>>> listQuestions(
            @PathVariable Long lessonId,
            @PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listQuestions(lessonId, quizId)));
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
            @PathVariable Long lessonId,
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.createQuestion(lessonId, quizId, req)));
    }

    @Operation(
            summary = "Cập nhật câu hỏi",
            description = "Sửa nội dung, lời giải thích, loại câu, orderIndex."
    )
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionWithOptionsDto>> updateQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateQuestion(lessonId, questionId, req)));
    }

    @Operation(summary = "Xoá câu hỏi", description = "Cascade xoá options; cập nhật lại tổng số câu.")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId) {
        service.deleteQuestion(lessonId, questionId);
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
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestBody List<@Valid OptionUpsertReq> reqs) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.addOptions(lessonId, questionId, reqs)));
    }

    @Operation(
            summary = "Cập nhật option",
            description = "Có thể đổi đáp án đúng; BE sẽ đảm bảo chỉ có 1 đáp án đúng."
    )
    @PutMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<OptionDto>> updateOption(
            @PathVariable Long lessonId,
            @PathVariable Long optionId,
            @Valid @RequestBody OptionUpsertReq req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.updateOption(lessonId, optionId, req)));
    }

    @Operation(
            summary = "Xoá option",
            description = "Không cho xoá nếu còn < 2 options hoặc đó là đáp án đúng duy nhất."
    )
    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable Long lessonId,
            @PathVariable Long optionId) {
        service.deleteOption(lessonId, optionId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
