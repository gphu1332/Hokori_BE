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
import org.springframework.http.ResponseEntity;
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

    // ===== Get progress cho 1 flashcard =====

    @GetMapping("/{flashcardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy trạng thái học của 1 flashcard",
            description = """
                    FE gọi API này để lấy progress của 1 flashcard cụ thể.
                    Trả về null nếu user chưa có progress, hoặc progress object nếu đã có.
                    
                    GET /api/flashcards/progress/{flashcardId}
                    
                    Response:
                    - Nếu chưa có progress → trả về null (204 No Content hoặc 200 với null)
                    - Nếu đã có progress → trả về UserFlashcardProgressResponse với:
                      {
                        "id": 1,
                        "userId": 123,
                        "flashcardId": 456,
                        "status": "MASTERED",
                        "masteredAt": "2024-01-01T00:00:00Z",
                        "lastReviewedAt": "2024-01-01T00:00:00Z",
                        "reviewCount": 5
                      }
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK - Trả về progress nếu có, null nếu chưa có",
            content = @Content(schema = @Schema(implementation = UserFlashcardProgressResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Flashcard not found"
    )
    public ResponseEntity<UserFlashcardProgressResponse> getProgress(@PathVariable Long flashcardId) {
        User current = currentUserService.getCurrentUserOrThrow();
        UserFlashcardProgressResponse progress = studyService.getProgress(current.getId(), flashcardId);
        
        if (progress == null) {
            return ResponseEntity.ok(null); // Return 200 with null body
        }
        
        return ResponseEntity.ok(progress);
    }

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
