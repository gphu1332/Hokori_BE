package com.hokori.web.controller;

import com.hokori.web.dto.progress.*;
import com.hokori.web.service.LearnerProgressService;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LEARNER')")
@Tag(name = "Learner - Progress", description = "Tiến độ học: chapter %, lesson ✓, content progress")
public class LearnerProgressController {

    private final LearnerProgressService progressService;
    private final CurrentUserService currentUser; // bạn đã có class này

    private Long uid() { return currentUser.getUserIdOrThrow(); }

    @Operation(summary = "Lấy enrollment lite của tôi với 1 course")
    @GetMapping("/courses/{courseId}/enrollment")
    public EnrollmentLiteRes getEnrollment(@PathVariable Long courseId) {
        return progressService.getEnrollment(uid(), courseId);
    }

    @Operation(summary = "Danh sách Chapter kèm % tiến độ")
    @GetMapping("/courses/{courseId}/chapters")
    public List<ChapterProgressRes> chapters(@PathVariable Long courseId) {
        return progressService.getChaptersProgress(uid(), courseId);
    }

    @Operation(summary = "Danh sách Lesson kèm trạng thái hoàn thành")
    @GetMapping("/courses/{courseId}/lessons")
    public List<LessonProgressRes> lessons(@PathVariable Long courseId) {
        return progressService.getLessonsProgress(uid(), courseId);
    }

    @Operation(summary = "Danh sách Contents của 1 lesson + tiến độ từng content")
    @GetMapping("/lessons/{lessonId}/contents")
    public List<ContentProgressRes> lessonContents(@PathVariable Long lessonId) {
        return progressService.getLessonContentsProgress(uid(), lessonId);
    }

    @Operation(summary = "Cập nhật tiến độ 1 content (resume/complete)")
    @PatchMapping("/contents/{contentId}/progress")
    public ContentProgressRes updateProgress(@PathVariable Long contentId,
                                             @RequestBody ContentProgressUpsertReq req) {
        return progressService.updateContentProgress(uid(), contentId, req);
    }
}
