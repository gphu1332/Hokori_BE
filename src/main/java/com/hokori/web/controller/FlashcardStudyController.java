// com.hokori.web.controller.FlashcardStudyController.java
package com.hokori.web.controller;

import com.hokori.web.dto.flashcard.FlashcardProgressUpdateRequest;
import com.hokori.web.dto.flashcard.UserFlashcardProgressResponse;
import com.hokori.web.entity.User;
import com.hokori.web.entity.UserFlashcardProgress;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.FlashcardStudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flashcards/progress")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
        name = "Flashcard Study",
        description = "API để cập nhật trạng thái học của flashcard (NEW / LEARNING / MASTERED)."
)
public class FlashcardStudyController {

    private final FlashcardStudyService studyService;
    private final CurrentUserService currentUserService;

    // ===== Update progress cho 1 flashcard =====

    @PostMapping("/{flashcardId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Cập nhật trạng thái học cho 1 flashcard",
            description = """
                    Khi learner học 1 thẻ và bấm:
                    - "Đã thuộc" → MASTERED
                    - "Chưa thuộc" → LEARNING (hoặc NEW)
                    
                    FE gọi:
                    POST /api/flashcards/progress/{flashcardId}
                    Body:
                    {
                      "status": "MASTERED"
                    }
                    
                    BE sẽ:
                    - Tạo mới record progress nếu chưa có.
                    - Tăng review_count.
                    - Cập nhật last_reviewed_at, mastered_at nếu cần.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cập nhật thành công",
            content = @Content(schema = @Schema(implementation = UserFlashcardProgressResponse.class))
    )
    public UserFlashcardProgressResponse updateProgress(
            @PathVariable Long flashcardId,
            @Valid @RequestBody FlashcardProgressUpdateRequest req
    ) {
        User current = currentUserService.getCurrentUserOrThrow();
        UserFlashcardProgress progress = studyService.updateProgress(
                current,
                flashcardId,
                req.getStatus()
        );
        return UserFlashcardProgressResponse.fromEntity(progress);
    }
}
