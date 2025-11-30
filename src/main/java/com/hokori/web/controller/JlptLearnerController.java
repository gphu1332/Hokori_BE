package com.hokori.web.controller;

import com.hokori.web.dto.jlpt.*;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.JlptTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learner/jlpt")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "JLPT - Learner", description = "Learner làm JLPT Test do Moderator tạo")
public class JlptLearnerController {

    private final JlptTestService jlptTestService;
    private final UserRepository userRepository;

    // TODO: dùng lại logic lấy current user id của bạn
    private Long currentUserIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }

        // Lấy email từ principal
        Object principal = auth.getPrincipal();
        String email;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails u) {
            email = u.getUsername();        // username = email
        } else {
            email = principal.toString();
        }

        var statusOpt = userRepository.findUserActiveStatusByEmail(email);
        if (statusOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        Object[] actual = statusOpt.get();          // [0] = id, [1] = isActive
        if (actual.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        Long userId = ((Number) actual[0]).longValue();

        boolean isActive;
        Object activeObj = actual[1];
        if (activeObj instanceof Boolean b) {
            isActive = b;
        } else if (activeObj instanceof Number n) {
            isActive = n.intValue() != 0;
        } else {
            isActive = "true".equalsIgnoreCase(activeObj.toString())
                    || "1".equals(activeObj.toString());
        }

        if (!isActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }

        return userId;
    }


    // 1) Learner "enroll" / bắt đầu 1 test
    @Operation(
            summary = "Learner bắt đầu làm JLPT Test",
            description = "Xoá answer cũ (nếu có) và trả đề + metadata để learner làm bài"
    )
    @PostMapping("/tests/{testId}/start")
    @PreAuthorize("hasRole('LEARNER')")
    public JlptTestStartResponse startTest(@PathVariable Long testId) {
        Long userId = currentUserIdOrThrow();
        return jlptTestService.startTest(testId, userId);
    }

    // 2) API lấy lại đề (nếu cần refresh giữa chừng)
    // Nếu bạn đã có GET /tests/{testId}/questions rồi thì có thể bỏ method này
    @Operation(
            summary = "Lấy danh sách câu hỏi + option cho 1 test",
            description = "Dùng cho Learner render đề (không bao gồm đáp án đúng)"
    )
    @GetMapping("/tests/{testId}/questions")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getQuestions(@PathVariable Long testId) {
        return jlptTestService.getQuestionsWithOptions(testId);
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
        Long userId = currentUserIdOrThrow();
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
        Long userId = currentUserIdOrThrow();
        return jlptTestService.getResultForUser(testId, userId);
    }

    // --- LISTENING ---
    @Operation(
            summary = "Lấy câu hỏi LISTENING của 1 JLPT test",
            description = "Trả về danh sách câu hỏi + options, questionType = LISTENING"
    )
    @GetMapping("/tests/{testId}/questions/listening")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getListeningQuestions(@PathVariable Long testId) {
        return jlptTestService.getListeningQuestions(testId);
    }

    // --- READING ---
    @Operation(
            summary = "Lấy câu hỏi READING của 1 JLPT test",
            description = "Trả về danh sách câu hỏi + options, questionType = READING"
    )
    @GetMapping("/tests/{testId}/questions/reading")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getReadingQuestions(@PathVariable Long testId) {
        return jlptTestService.getReadingQuestions(testId);
    }

    // --- GRAMMAR + VOCAB ---
    @Operation(
            summary = "Lấy câu hỏi GRAMMAR + VOCAB của 1 JLPT test",
            description = "Gom 2 loại GRAMMAR và VOCAB, FE hiển thị chung phần Ngữ pháp & Từ vựng"
    )
    @GetMapping("/tests/{testId}/questions/grammar-vocab")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptQuestionWithOptionsResponse> getGrammarVocabQuestions(@PathVariable Long testId) {
        return jlptTestService.getGrammarVocabQuestions(testId);
    }

    @Operation(
            summary = "Learner lấy danh sách đề JLPT theo event",
            description = "Trả về list các đề JLPT (mock test) thuộc 1 event: level, thời gian, điểm đậu..."
    )
    @GetMapping("/tests")
    @PreAuthorize("hasRole('LEARNER')")
    public List<JlptTestListItemResponse> listTests(
            @RequestParam("eventId") Long eventId
    ) {
        return jlptTestService.listTestsForLearner(eventId);
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
}
