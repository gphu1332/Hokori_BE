package com.hokori.web.controller;

import com.hokori.web.dto.jlpt.*;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.JlptTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learner/jlpt")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "JLPT - Learner", description = "Learner làm JLPT Test do Moderator tạo")
public class JlptLearnerController {

    private final JlptTestService jlptTestService;
    private final CurrentUserService currentUserService;

    // 1) Learner "enroll" / bắt đầu 1 test
    @Operation(
            summary = "Learner bắt đầu làm JLPT Test",
            description = "Xoá answer cũ (nếu có) và trả đề + metadata để learner làm bài"
    )
    @PostMapping("/tests/{testId}/start")
    @PreAuthorize("hasRole('LEARNER')")
    public JlptTestStartResponse startTest(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.startTest(testId, userId);
    }

    // 2) API lấy lại đề (nếu cần refresh giữa chừng)
    @Operation(
            summary = "Lấy danh sách câu hỏi + option cho 1 test",
            description = """
                    Dùng cho Learner render đề (không bao gồm đáp án đúng).
                    Trả về cả selectedOptionId nếu user đã chọn đáp án trước đó.
                    Khi FE refresh (F5), gọi endpoint này để restore các câu đã chọn.
                    """
    )
    @GetMapping("/tests/{testId}/questions")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getQuestions(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.getQuestionsWithOptions(testId, userId);
    }

    // 3) Learner nộp đáp án cho 1 câu hỏi
    @Operation(
            summary = "Learner nộp đáp án cho 1 câu hỏi",
            description = "1 user chỉ có 1 answer / câu hỏi, nộp lại sẽ ghi đè"
    )
    @PostMapping("/tests/{testId}/answers")
    @PreAuthorize("hasRole('LEARNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitAnswer(@PathVariable Long testId,
                             @Valid @RequestBody JlptAnswerSubmitRequest req) {
        Long userId = currentUserService.getUserIdOrThrow();
        jlptTestService.submitAnswer(testId, userId, req);
    }

    // 4) Learner xem kết quả của chính mình
    @Operation(
            summary = "Learner xem kết quả JLPT Test của chính mình",
            description = "Trả tổng số câu, số câu đúng và điểm quy đổi"
    )
    @GetMapping("/tests/{testId}/my-result")
    @PreAuthorize("hasRole('LEARNER')")
    public JlptTestResultResponse getMyResult(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.getResultForUser(testId, userId);
    }

    // --- LISTENING ---
    @Operation(
            summary = "Lấy câu hỏi LISTENING của 1 JLPT test",
            description = """
                    Trả về danh sách câu hỏi + options, questionType = LISTENING
                    
                    ⚠️ QUAN TRỌNG: 
                    - Endpoint này yêu cầu role LEARNER trong JWT token
                    - Audio files được serve qua /files/** (public, không cần authentication)
                    - AudioUrl trong response đã có prefix /files/ để FE có thể dùng trực tiếp
                    """
    )
    @GetMapping("/tests/{testId}/questions/listening")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getListeningQuestions(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.getListeningQuestions(testId, userId);
    }

    // --- READING ---
    @Operation(
            summary = "Lấy câu hỏi READING của 1 JLPT test",
            description = "Trả về danh sách câu hỏi + options, questionType = READING"
    )
    @GetMapping("/tests/{testId}/questions/reading")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getReadingQuestions(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.getReadingQuestions(testId, userId);
    }

    // --- GRAMMAR + VOCAB ---
    @Operation(
            summary = "Lấy câu hỏi GRAMMAR + VOCAB của 1 JLPT test",
            description = "Gom 2 loại GRAMMAR và VOCAB, FE hiển thị chung phần Ngữ pháp & Từ vựng"
    )
    @GetMapping("/tests/{testId}/questions/grammar-vocab")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getGrammarVocabQuestions(@PathVariable Long testId) {
        Long userId = currentUserService.getUserIdOrThrow();
        return jlptTestService.getGrammarVocabQuestions(testId, userId);
    }

    @Operation(
            summary = "Lấy số người đang làm JLPT Test theo thời gian thực (dùng cho polling)",
            description = "FE có thể gọi mỗi 3s để cập nhật số người đang làm bài"
    )
    @GetMapping("/tests/{testId}/active-users")
    @PreAuthorize("hasRole('LEARNER')")
    public Map<String, Long> getActiveUsers(@PathVariable Long testId) {
        long count = jlptTestService.getActiveUserCount(testId);
        return Map.of("activeUsers", count);
    }

    @Operation(
            summary = "Learner lấy danh sách đề JLPT theo event",
            description = "Trả về list đề JLPT (mock test) của 1 JLPT Event: level, thời gian, điểm đậu..."
    )
    @GetMapping("/events/{eventId}/tests")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptTestListItemResponse> listTestsByEvent(@PathVariable Long eventId) {
        return jlptTestService.listTestsForLearner(eventId);
    }
}
