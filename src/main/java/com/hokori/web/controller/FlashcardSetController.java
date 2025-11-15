// com.hokori.web.controller.FlashcardSetController.java
package com.hokori.web.controller;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.dto.flashcard.CourseVocabSetCreateRequest;
import com.hokori.web.dto.flashcard.FlashcardCreateRequest;
import com.hokori.web.dto.flashcard.FlashcardResponse;
import com.hokori.web.dto.flashcard.FlashcardSetCreateRequest;
import com.hokori.web.dto.flashcard.FlashcardSetResponse;
import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.FlashcardSet;
import com.hokori.web.entity.SectionsContent;
import com.hokori.web.entity.User;
import com.hokori.web.repository.FlashcardSetRepository;
import com.hokori.web.repository.SectionsContentRepository;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.service.FlashcardSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flashcards/sets")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
        name = "Flashcard Sets",
        description = """
                API cho luồng flashcard:
                1) Learner tự tạo bộ flashcard cá nhân (PERSONAL).
                2) Teacher tạo bộ flashcard cho session từ vựng (COURSE_VOCAB) và gắn với SectionsContent.
                """
)
public class FlashcardSetController {

    private final FlashcardSetService flashcardSetService;
    private final FlashcardSetRepository setRepo;
    private final UserRepository userRepo;
    private final SectionsContentRepository sectionsContentRepo;

    // ===== Helper: current user =====

    private User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthenticated");
        }
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private Long getCurrentUserIdOrThrow() {
        return getCurrentUserOrThrow().getId();
    }

    // ===== 1. Learner tạo set cá nhân =====

    @PostMapping("/personal")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Tạo flashcard set cá nhân (PERSONAL)",
            description = """
                    Dùng cho learner tự tạo bộ flashcard cho chính mình (giống Anki).
                    - type của set sẽ là PERSONAL.
                    
                    Request body:
                    {
                      "title": "N5 Từ vựng bài 1",
                      "description": "Bộ tự tạo cho cá nhân",
                      "level": "N5"
                    }
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tạo set thành công",
                    content = @Content(schema = @Schema(implementation = FlashcardSetResponse.class))
            )
    })
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
    public FlashcardSetResponse createPersonalSet(
            @Valid @RequestBody FlashcardSetCreateRequest req
    ) {
        User current = getCurrentUserOrThrow();
        FlashcardSet set = flashcardSetService.createPersonalSet(
                current,
                req.getTitle(),
                req.getDescription(),
                req.getLevel()
        );
        return FlashcardSetResponse.fromEntity(set);
    }

    // ===== 2. Teacher tạo set cho session từ vựng (gắn SectionsContent) =====

    @PostMapping("/course-vocab")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Teacher tạo flashcard set cho session từ vựng (COURSE_VOCAB)",
            description = """
                    Dùng cho teacher tạo bộ flashcard gắn với 1 SectionsContent (session/bài học) có type 'từ vựng'.
                    - type của set sẽ là COURSE_VOCAB.
                    - FlashcardSet được gắn trực tiếp với SectionsContent qua field section_content_id.
                    
                    Request body:
                    {
                      "title": "Từ vựng bài 3",
                      "description": "Flashcard cho lesson 3",
                      "level": "N4",
                      "sectionContentId": 123
                    }
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tạo set thành công",
                    content = @Content(schema = @Schema(implementation = FlashcardSetResponse.class))
            )
    })
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
    public FlashcardSetResponse createCourseVocabSet(
            @Valid @RequestBody CourseVocabSetCreateRequest req
    ) {
        User current = getCurrentUserOrThrow();

        // 1. Lấy SectionsContent mà teacher muốn gắn flashcard
        SectionsContent sectionContent = sectionsContentRepo.findById(req.getSectionContentId())
                .orElseThrow(() -> new EntityNotFoundException("SectionsContent not found"));

        // TODO (optional): kiểm tra current có phải owner của course/section này không

        // 2. Tạo set type COURSE_VOCAB và gắn với sectionContent
        FlashcardSet set = flashcardSetService.createCourseVocabSet(
                current,
                sectionContent,
                req.getTitle(),
                req.getDescription(),
                req.getLevel()
        );
        return FlashcardSetResponse.fromEntity(set);
    }

    // ===== 3. Lấy tất cả set do mình tạo (learner/teacher) =====

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách flashcard set do user hiện tại tạo",
            description = """
                    - Có thể filter theo type:
                      + PERSONAL: set tự tạo.
                      + COURSE_VOCAB: set tạo cho khoá học.
                    - Nếu không truyền type thì trả tất cả set do user hiện tại tạo.
                    
                    Ví dụ:
                    GET /api/flashcards/sets/me?type=PERSONAL
                    GET /api/flashcards/sets/me
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = FlashcardSetResponse.class))
                    )
            )
    })
    public List<FlashcardSetResponse> getMySets(
            @Parameter(
                    description = "Loại set: PERSONAL hoặc COURSE_VOCAB (optional)"
            )
            @RequestParam(required = false) FlashcardSetType type
    ) {
        Long userId = getCurrentUserIdOrThrow();
        List<FlashcardSet> sets;
        if (type != null) {
            sets = setRepo.findByCreatedBy_IdAndTypeAndDeletedFlagFalse(userId, type);
        } else {
            sets = setRepo.findByCreatedBy_IdAndDeletedFlagFalse(userId);
        }
        return sets.stream()
                .map(FlashcardSetResponse::fromEntity)
                .toList();
    }

    // ===== 4. Get detail 1 set =====

    @GetMapping("/{setId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin chi tiết 1 flashcard set",
            description = """
                    Dùng để FE hiển thị thông tin set (tiêu đề, mô tả...) trước khi vào màn học.
                    """
    )
    public FlashcardSetResponse getSetDetail(
            @PathVariable Long setId
    ) {
        FlashcardSet set = flashcardSetService.getSetOrThrow(setId);
        return FlashcardSetResponse.fromEntity(set);
    }

    // ===== 5. Thêm card vào set (owner) =====

    @PostMapping("/{setId}/cards")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Thêm 1 flashcard vào set",
            description = """
                    - Learner: thêm card vào set PERSONAL của chính mình.
                    - Teacher: thêm card vào set COURSE_VOCAB do mình tạo.
                    
                    Điều kiện:
                    - Chỉ chủ sở hữu set (created_by_user_id) mới được thêm card.
                    """
    )
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
    public FlashcardResponse addCard(
            @PathVariable Long setId,
            @Valid @RequestBody FlashcardCreateRequest req
    ) {
        User current = getCurrentUserOrThrow();
        FlashcardSet set = flashcardSetService.getSetOrThrow(setId);
        if (!set.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not the owner of this flashcard set");
        }

        Flashcard card = flashcardSetService.addCardToSet(
                setId,
                req.getFrontText(),
                req.getBackText(),
                req.getReading(),
                req.getExampleSentence(),
                req.getOrderIndex()
        );
        return FlashcardResponse.fromEntity(card);
    }

    // ===== 6. Lấy danh sách card trong 1 set =====

    @GetMapping("/{setId}/cards")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy tất cả flashcard của 1 set",
            description = """
                    Dùng cho màn học:
                    - FE gọi API này để lấy list card trong 1 set (cho learner hoặc teacher).
                    - Kết hợp với API progress để biết card nào đã MASTERED.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = FlashcardResponse.class))
                    )
            )
    })
    public List<FlashcardResponse> listCards(
            @PathVariable Long setId
    ) {
        List<Flashcard> cards = flashcardSetService.listCards(setId);
        return cards.stream()
                .map(FlashcardResponse::fromEntity)
                .toList();
    }

    // ===== 7. Lấy set gắn với 1 SectionsContent (dùng cho FE khi mở session từ vựng) =====

    @GetMapping("/by-section-content/{sectionContentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy flashcard set (COURSE_VOCAB) gắn với 1 SectionsContent",
            description = """
                    FE dùng khi user mở 1 session type 'từ vựng':
                    - Input: sectionContentId (id của session).
                    - Output: thông tin FlashcardSet nếu đã tạo.
                    
                    Ví dụ:
                    GET /api/flashcards/sets/by-section-content/123
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tìm thấy set",
                    content = @Content(schema = @Schema(implementation = FlashcardSetResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Session chưa có flashcard set")
    })
    public FlashcardSetResponse getSetBySectionContent(
            @PathVariable Long sectionContentId
    ) {
        FlashcardSet set = setRepo.findBySectionContent_IdAndDeletedFlagFalse(sectionContentId)
                .orElseThrow(() -> new EntityNotFoundException("FlashcardSet not found for this sectionContent"));
        return FlashcardSetResponse.fromEntity(set);
    }
}
