// com.hokori.web.controller.JlptTestController.java
package com.hokori.web.controller;

import com.hokori.web.dto.jlpt.*;
import com.hokori.web.entity.JlptEvent;
import com.hokori.web.entity.JlptTest;
import com.hokori.web.entity.User;
import com.hokori.web.repository.JlptEventRepository;
import com.hokori.web.repository.JlptTestRepository;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.JlptTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jlpt")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "JLPT Tests", description = "Quản lý đề thi, câu hỏi & làm bài JLPT")
public class JlptTestController {

    private final JlptTestService jlptTestService;
    private final JlptEventRepository eventRepo;
    private final JlptTestRepository testRepo;
    private final CurrentUserService currentUserService;

    // ===== Moderator: tạo test trong 1 event =====

    @PostMapping("/events/{eventId}/tests")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(
            summary = "Moderator tạo JLPT Test cho 1 Event",
            description = """
                    - Chỉ moderator mới tạo được đề.
                    - eventId: sự kiện mà đề thuộc về.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tạo test thành công",
            content = @Content(schema = @Schema(implementation = JlptTestResponse.class))
    )
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public JlptTestResponse createTest(
            @PathVariable Long eventId,
            @Valid @RequestBody JlptTestCreateRequest req
    ) {
        User moderator = currentUserService.getCurrentUserOrThrow();
        JlptEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        JlptTest test = jlptTestService.createTest(event, moderator, req);
        return JlptTestResponse.fromEntity(test);
    }

    // ===== Moderator/Admin: list test trong 1 event =====

    @GetMapping("/events/{eventId}/tests")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(
            summary = "List test của 1 event (Admin/Moderator)",
            description = "Dùng cho màn quản lý đề."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = JlptTestResponse.class)))
    )
    public List<JlptTestResponse> listTestsByEvent(
            @PathVariable Long eventId
    ) {
        List<JlptTest> tests = testRepo.findByEvent_IdAndDeletedFlagFalse(eventId);
        return tests.stream()
                .map(JlptTestResponse::fromEntity)
                .toList();
    }

    // ===== Moderator: thêm câu hỏi cho test =====

    @PostMapping("/tests/{testId}/questions")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(
            summary = "Moderator thêm câu hỏi cho test",
            description = """
                    Tạo 1 JLPT Question thuộc về test:
                    - Content, questionType, orderIndex
                    - audioPath, imagePath nếu là câu nghe / có hình.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tạo câu hỏi thành công",
            content = @Content(schema = @Schema(implementation = JlptQuestionWithOptionsResponse.class))
    )
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public JlptQuestionWithOptionsResponse createQuestion(
            @PathVariable Long testId,
            @Valid @RequestBody JlptQuestionCreateRequest req
    ) {
        User moderator = currentUserService.getCurrentUserOrThrow();
        JlptQuestionWithOptionsResponse question =
                jlptTestService.createQuestion(testId, moderator, req);
        return question;
    }

    // ===== Moderator: thêm option cho 1 câu hỏi =====

    @PostMapping("/questions/{questionId}/options")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(
            summary = "Moderator thêm option cho câu hỏi",
            description = """
                    Tạo JLPT Option:
                    - content, orderIndex, correct flag
                    - imagePath nếu đáp án là hình.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tạo option thành công",
            content = @Content(schema = @Schema(implementation = JlptOptionResponse.class))
    )
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public JlptOptionResponse createOption(
            @PathVariable Long questionId,
            @Valid @RequestBody JlptOptionCreateRequest req
    ) {
        User moderator = currentUserService.getCurrentUserOrThrow();
        return jlptTestService.createOption(questionId, moderator, req);
    }

    // ===== Learner / Moderator: lấy full đề (questions + options) để làm =====

    @GetMapping("/tests/{testId}/questions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy full câu hỏi + option cho 1 test",
            description = """
                    FE dùng để hiển thị đề thi:
                    - Trả về list câu hỏi kèm list options.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = JlptQuestionWithOptionsResponse.class)))
    )
    public List<JlptQuestionWithOptionsResponse> getQuestionsForTest(
            @PathVariable Long testId
    ) {
        return jlptTestService.getQuestionsWithOptions(testId);
    }

    // ===== Learner: submit đáp án cho 1 câu hỏi =====

    @PostMapping("/tests/{testId}/answers")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Learner nộp đáp án cho 1 câu hỏi",
            description = """
                    FE gọi mỗi lần user chọn/đổi đáp án:
                    POST /api/jlpt/tests/{testId}/answers
                    {
                      "questionId": 123,
                      "selectedOptionId": 456
                    }
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lưu answer thành công"
    )
    public void submitAnswer(
            @PathVariable Long testId,
            @Valid @RequestBody JlptAnswerSubmitRequest req
    ) {
        Long userId = currentUserService.getCurrentUserId();
        jlptTestService.submitAnswer(testId, userId, req);
    }

    // ===== Learner: xem kết quả test của chính mình =====

    @GetMapping("/tests/{testId}/my-result")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Learner xem kết quả test của chính mình",
            description = """
                    Tính tổng số câu đúng, điểm tương ứng:
                    - score = totalScore * correctCount / totalQuestions
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = JlptTestResultResponse.class))
    )
    public JlptTestResultResponse getMyResult(
            @PathVariable Long testId
    ) {
        Long userId = currentUserService.getCurrentUserId();
        return jlptTestService.getResultForUser(testId, userId);
    }
}
