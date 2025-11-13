package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.quiz.*;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.LearnerQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Swagger tối giản
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@PreAuthorize("hasRole('LEARNER')")
@RestController
@RequestMapping("/api/learner/lessons/{lessonId}/quiz")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Learner: Quiz Play")
@SecurityRequirement(name = "Bearer Authentication")
public class LearnerQuizController {

    private final LearnerQuizService service;
    private final CurrentUserService current;

    private Long me(){ return current.getCurrentUserOrThrow().getId(); }

    @Operation(
            summary = "Bắt đầu (hoặc tiếp tục) một attempt",
            description = """
            Trả về attempt đang IN_PROGRESS nếu có (trừ khi forceNew=true).
            Ràng buộc tuỳ chọn: phải enroll.
            """
    )
    @PostMapping("/attempts/start")
    public ResponseEntity<ApiResponse<AttemptDto>> start(
            @PathVariable Long lessonId,
            @RequestBody(required = false) StartAttemptReq req){
        return ResponseEntity.ok(ApiResponse.success("OK", service.startAttempt(lessonId, me(), req)));
    }

    @Operation(summary = "Lấy câu hỏi kế tiếp chưa làm (null = hết câu)")
    @GetMapping("/attempts/{attemptId}/next")
    public ResponseEntity<ApiResponse<PlayQuestionDto>> next(
            @PathVariable Long lessonId, @PathVariable Long attemptId){
        return ResponseEntity.ok(ApiResponse.success("OK", service.nextQuestion(attemptId, me())));
    }

    @Operation(summary = "Trả lời một câu (SINGLE_CHOICE)")
    @PostMapping("/attempts/{attemptId}/questions/{questionId}/answer")
    public ResponseEntity<ApiResponse<Void>> answer(
            @PathVariable Long lessonId, @PathVariable Long attemptId, @PathVariable Long questionId,
            @Valid @RequestBody AnswerReq req){
        service.answer(attemptId, me(), questionId, req);
        return ResponseEntity.ok(ApiResponse.success("Saved", null));
    }

    @Operation(summary = "Nộp bài và chấm điểm")
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<AttemptDto>> submit(
            @PathVariable Long lessonId, @PathVariable Long attemptId){
        return ResponseEntity.ok(ApiResponse.success("Submitted", service.submit(attemptId, me())));
    }

    @Operation(summary = "Xem chi tiết 1 attempt (đáp án đã chọn, đúng/sai)")
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<ApiResponse<LearnerQuizService.AttemptDetailDto>> detail(
            @PathVariable Long lessonId, @PathVariable Long attemptId){
        return ResponseEntity.ok(ApiResponse.success("OK", service.detail(attemptId, me())));
    }

    @Operation(summary = "Lịch sử làm bài theo lesson")
    @GetMapping("/attempts")
    public ResponseEntity<ApiResponse<List<AttemptDto>>> history(@PathVariable Long lessonId){
        return ResponseEntity.ok(ApiResponse.success("OK", service.history(lessonId, me())));
    }
}
