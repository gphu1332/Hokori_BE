// com.hokori.web.controller.FlashcardSetController.java
package com.hokori.web.controller;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.dto.flashcard.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.FlashcardSetRepository;
import com.hokori.web.repository.SectionsContentRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.FlashcardSetService;
import com.hokori.web.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
import org.springframework.transaction.annotation.Transactional;
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
    private final CurrentUserService currentUserService;
    private final SectionsContentRepository sectionsContentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final CourseService courseService;

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
        User current = currentUserService.getCurrentUserOrThrow();
        FlashcardSet set = flashcardSetService.createPersonalSet(
                current,
                req.getTitle(),
                req.getDescription(),
                req.getLevel()
        );
        // Reload with eager fetching to avoid LazyInitializationException
        set = flashcardSetService.getSetOrThrowWithCreatedBy(set.getId());
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
        User current = currentUserService.getCurrentUserOrThrow();

        // 1. Lấy SectionsContent mà teacher muốn gắn flashcard
        SectionsContent sectionContent = sectionsContentRepo.findById(req.getSectionContentId())
                .orElseThrow(() -> new EntityNotFoundException("SectionsContent not found"));

        // 2. Kiểm tra teacher có phải owner của course này không
        Long courseOwnerId = sectionsContentRepo.findCourseOwnerIdBySectionContentId(req.getSectionContentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found for this section content"));
        
        if (!courseOwnerId.equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You are not the owner of this course. Only the course owner can create flashcard sets for course vocabulary.");
        }

        // 2.5. Check if there's already an active flashcard set for this sectionContent
        // If exists, soft delete the old one(s) before creating a new one
        List<FlashcardSet> existingSets = setRepo.findBySectionContent_IdAndDeletedFlagFalseWithCreatedBy(req.getSectionContentId());
        if (!existingSets.isEmpty()) {
            // Soft delete all existing sets for this sectionContent
            for (FlashcardSet existingSet : existingSets) {
                flashcardSetService.softDeleteSet(existingSet.getId());
            }
        }

        // 3. Tạo set type COURSE_VOCAB và gắn với sectionContent
        FlashcardSet set = flashcardSetService.createCourseVocabSet(
                current,
                sectionContent,
                req.getTitle(),
                req.getDescription(),
                req.getLevel()
        );
        // Reload with eager fetching to avoid LazyInitializationException
        set = flashcardSetService.getSetOrThrowWithCreatedBy(set.getId());
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
        Long userId = currentUserService.getCurrentUserId();
        List<FlashcardSet> sets;
        if (type != null) {
            sets = setRepo.findByCreatedBy_IdAndTypeAndDeletedFlagFalseWithCreatedBy(userId, type);
        } else {
            sets = setRepo.findByCreatedBy_IdAndDeletedFlagFalseWithCreatedBy(userId);
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
                    - PERSONAL: chỉ cho phép owner
                    - COURSE_VOCAB: cho phép teacher (owner), learner (đã enroll), hoặc moderator (nếu course pending approval)
                    """
    )
    public FlashcardSetResponse getSetDetail(
            @PathVariable Long setId
    ) {
        Long currentUserId = currentUserService.getUserIdOrThrow();
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);
        
        // Authorization check
        if (set.getType() == FlashcardSetType.PERSONAL) {
            // PERSONAL: chỉ cho phép owner
            if (!set.getCreatedBy().getId().equals(currentUserId)) {
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        } else if (set.getType() == FlashcardSetType.COURSE_VOCAB) {
            // COURSE_VOCAB: check enrollment or ownership
            // First, allow if user is the creator of the set (fallback for edge cases)
            if (set.getCreatedBy().getId().equals(currentUserId)) {
                // User created this set, allow access
                return FlashcardSetResponse.fromEntity(set);
            }
            
            // Then check via sectionContent if available
            if (set.getSectionContent() != null) {
                Long sectionContentId = set.getSectionContent().getId();
                
                // Allow moderator if course is pending approval
                if (currentUserService.hasRole("MODERATOR")) {
                    try {
                        courseService.requireSectionContentBelongsToPendingApprovalCourse(sectionContentId);
                        return FlashcardSetResponse.fromEntity(set);
                    } catch (ResponseStatusException e) {
                        // Not pending approval, continue with normal check
                    }
                }
                
                // Check if user is course owner (teacher)
                Long courseOwnerId = sectionsContentRepo.findCourseOwnerIdBySectionContentId(sectionContentId)
                        .orElse(null);
                
                if (courseOwnerId != null && courseOwnerId.equals(currentUserId)) {
                    // Teacher owner, allow access
                    return FlashcardSetResponse.fromEntity(set);
                }
                
                // Not owner, check if learner is enrolled
                Long courseId = sectionsContentRepo.findCourseIdBySectionContentId(sectionContentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found for this section content"));
                
                enrollmentRepo.findByUserIdAndCourseId(currentUserId, courseId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You must enroll in this course to access flashcard sets"));
            } else {
                // SectionContent is null but set is COURSE_VOCAB - this shouldn't happen normally
                // But if it does, only allow creator
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        }
        
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
        User current = currentUserService.getCurrentUserOrThrow();
        // Use method with eager fetching to avoid LazyInitializationException
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);
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


    // ===== 5.1 Cập nhật metadata của 1 flashcard set (title/description/level) =====

    @PutMapping("/{setId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật flashcard set (title / description / level)",
            description = """
                - Learner: chỉ được sửa set PERSONAL của chính mình.
                - Teacher: chỉ được sửa set COURSE_VOCAB do mình tạo.

                Cập nhật metadata:
                - title
                - description
                - level (JLPT)
                """
    )
    public FlashcardSetResponse updateSet(
            @PathVariable Long setId,
            @Valid @RequestBody FlashcardSetUpdateRequest req
    ) {
        User current = currentUserService.getCurrentUserOrThrow();
        // Use method with eager fetching to avoid LazyInitializationException
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);

        if (!set.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not the owner of this flashcard set");
        }

        FlashcardSet updated = flashcardSetService.updateSet(
                setId,
                req.title(),
                req.description(),
                req.level()
        );
        // Reload with eager fetching to avoid LazyInitializationException
        updated = flashcardSetService.getSetOrThrowWithCreatedBy(updated.getId());
        return FlashcardSetResponse.fromEntity(updated);
    }

    // ===== 5.2 Cập nhật 1 flashcard trong set =====

    @PutMapping("/{setId}/cards/{cardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật 1 flashcard trong set",
            description = """
                - Learner: chỉ sửa card trong set PERSONAL của chính mình.
                - Teacher: chỉ sửa card trong set COURSE_VOCAB do mình tạo.

                Điều kiện:
                - Chỉ chủ sở hữu set (created_by_user_id) mới được sửa card.
                """
    )
    public FlashcardResponse updateCard(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @Valid @RequestBody FlashcardUpdateRequest req
    ) {
        User current = currentUserService.getCurrentUserOrThrow();
        // Use method with eager fetching to avoid LazyInitializationException
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);

        if (!set.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not the owner of this flashcard set");
        }

        Flashcard card = flashcardSetService.updateCardInSet(
                setId,
                cardId,
                req.frontText(),
                req.backText(),
                req.reading(),
                req.exampleSentence(),
                req.orderIndex()
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
        // Use method with eager fetching to avoid LazyInitializationException
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);
        Long currentUserId = currentUserService.getUserIdOrThrow();
        
        // Authorization check for COURSE_VOCAB sets
        if (set.getType() == FlashcardSetType.COURSE_VOCAB) {
            // First, allow if user is the creator of the set (fallback for edge cases)
            if (set.getCreatedBy().getId().equals(currentUserId)) {
                // User created this set, allow access
            } else if (set.getSectionContent() != null) {
                Long sectionContentId = set.getSectionContent().getId();
                boolean authorized = false;
                
                // Allow moderator if course is pending approval
                if (currentUserService.hasRole("MODERATOR")) {
                    try {
                        courseService.requireSectionContentBelongsToPendingApprovalCourse(sectionContentId);
                        // Validated, moderator can access
                        authorized = true;
                    } catch (ResponseStatusException e) {
                        // Not pending approval, continue with normal check
                    }
                }
                
                // If not authorized by moderator, check other permissions
                if (!authorized) {
                    // Check if user is course owner (teacher)
                    Long courseOwnerId = sectionsContentRepo.findCourseOwnerIdBySectionContentId(sectionContentId)
                            .orElse(null);
                    
                    if (courseOwnerId != null && courseOwnerId.equals(currentUserId)) {
                        // Teacher owner, allow access
                        authorized = true;
                    } else {
                        // Not owner, check if learner is enrolled
                        Long courseId = sectionsContentRepo.findCourseIdBySectionContentId(sectionContentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found for this section content"));
                        
                        enrollmentRepo.findByUserIdAndCourseId(currentUserId, courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "You must enroll in this course to access flashcard cards"));
                        authorized = true;
                    }
                }
            } else {
                // SectionContent is null but set is COURSE_VOCAB - only allow creator
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        }
        
        List<Flashcard> cards = flashcardSetService.listCards(setId);
        return cards.stream()
                .map(FlashcardResponse::fromEntity)
                .toList();
    }

    // ===== 7. Lấy set gắn với 1 SectionsContent (dùng cho FE khi mở session từ vựng) =====

    @GetMapping("/by-section-content/{sectionContentId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
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
        Long currentUserId = currentUserService.getUserIdOrThrow();
        
        // Get flashcard set with eager fetching to avoid LazyInitializationException
        // This query eagerly fetches: createdBy, createdBy.role, and sectionContent
        // If multiple sets exist, get the most recent one (ORDER BY createdAt DESC)
        List<FlashcardSet> sets = setRepo.findBySectionContent_IdAndDeletedFlagFalseWithCreatedBy(sectionContentId);
        if (sets.isEmpty()) {
            throw new EntityNotFoundException("FlashcardSet not found for this sectionContent");
        }
        // Get the most recent set (first in list due to ORDER BY createdAt DESC)
        FlashcardSet set = sets.get(0);
        
        // Force initialize all lazy relationships within transaction to avoid LazyInitializationException
        // This ensures all data is loaded before transaction closes and Jackson serializes
        User createdBy = set.getCreatedBy();
        if (createdBy != null) {
            // Force load createdBy
            Long userId = createdBy.getId();
            // Force load role
            Role role = createdBy.getRole();
            if (role != null) {
                Long roleId = role.getId();
                String roleName = role.getRoleName(); // Access all fields to ensure full initialization
            }
        }
        SectionsContent sectionContent = set.getSectionContent();
        if (sectionContent != null) {
            // Force load sectionContent
            Long sectionContentIdLoaded = sectionContent.getId();
        }
        
        // Validation: 
        // - Nếu là COURSE_VOCAB: chỉ cho phép teacher (owner của course), learner (đã enroll), hoặc moderator (nếu course pending approval)
        // - Nếu là PERSONAL: chỉ cho phép người tạo
        if (set.getType() == FlashcardSetType.COURSE_VOCAB && set.getSectionContent() != null) {
            // Allow moderator if course is pending approval
            if (currentUserService.hasRole("MODERATOR")) {
                try {
                    courseService.requireSectionContentBelongsToPendingApprovalCourse(sectionContentId);
                    return FlashcardSetResponse.fromEntity(set);
                } catch (ResponseStatusException e) {
                    // Not pending approval, continue with normal check
                }
            }
            
            // Check nếu teacher là owner của course
            Long courseOwnerId = sectionsContentRepo.findCourseOwnerIdBySectionContentId(sectionContentId)
                    .orElse(null);
            
            if (courseOwnerId != null && courseOwnerId.equals(currentUserId)) {
                // Teacher owner, allow access
            } else {
                // Not owner, check if learner is enrolled
                Long courseId = sectionsContentRepo.findCourseIdBySectionContentId(sectionContentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found for this section content"));
                
                enrollmentRepo.findByUserIdAndCourseId(currentUserId, courseId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You must enroll in this course to access flashcard sets"));
            }
        } else if (set.getType() == FlashcardSetType.PERSONAL) {
            // PERSONAL: chỉ cho phép người tạo
            if (!set.getCreatedBy().getId().equals(currentUserId)) {
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        }
        
        return FlashcardSetResponse.fromEntity(set);
    }

    // ===== 8. Xoá 1 flashcard trong set =====

    @DeleteMapping("/{setId}/cards/{cardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xoá 1 flashcard trong set",
            description = """
                - Learner: chỉ được xoá card trong set PERSONAL của chính mình.
                - Teacher: chỉ được xoá card trong set COURSE_VOCAB do mình tạo.

                Điều kiện:
                - Chỉ chủ sở hữu set (created_by_user_id) mới được xoá card.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xoá flashcard thành công"),
            @ApiResponse(responseCode = "403", description = "Không phải owner của set"),
            @ApiResponse(responseCode = "404", description = "Set hoặc flashcard không tồn tại")
    })
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteCard(
            @PathVariable Long setId,
            @PathVariable Long cardId
    ) {
        User current = currentUserService.getCurrentUserOrThrow();

        // 1. Lấy set và check owner (use eager fetching to avoid LazyInitializationException)
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);
        if (!set.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not the owner of this flashcard set");
        }

        // 2. Xoá card trong set (business logic ở service)
        flashcardSetService.deleteCardFromSet(setId, cardId);
    }

    // ===== 9. Xoá 1 flashcard set (soft delete) =====

    @DeleteMapping("/{setId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xoá 1 flashcard set (soft delete)",
            description = """
                - Learner: chỉ được xoá set PERSONAL do chính mình tạo.
                - Teacher: chỉ được xoá set COURSE_VOCAB do mình tạo.
                
                Thực hiện xoá mềm:
                - Đặt deleted_flag = true cho set.
                - (Tuỳ chọn) xoá mềm luôn các card bên trong set.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xoá set thành công"),
            @ApiResponse(responseCode = "403", description = "Không phải owner của set"),
            @ApiResponse(responseCode = "404", description = "Set không tồn tại")
    })
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteSet(
            @PathVariable Long setId
    ) {
        User current = currentUserService.getCurrentUserOrThrow();

        // 1. Lấy set (use eager fetching to avoid LazyInitializationException)
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);

        // 2. Check owner
        if (!set.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not the owner of this flashcard set");
        }

        // 3. Soft delete set (và card nếu muốn)
        flashcardSetService.softDeleteSet(setId);
    }

    // ===== NEW: Dashboard =====
    @GetMapping("/dashboard/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Dashboard flashcard của user hiện tại",
            description = """
                    Trả về số bộ thẻ, tổng số thẻ, số thẻ đã ôn hôm nay và chuỗi ngày học.
                    Có thể filter theo cấp độ JLPT của bộ thẻ.
                    
                    Ví dụ:
                    GET /api/flashcards/sets/dashboard/me
                    GET /api/flashcards/sets/dashboard/me?level=N5
                    """
    )
    public FlashcardDashboardResponse getMyFlashcardDashboard(
            @RequestParam(required = false)
            @Parameter(description = "Lọc theo JLPT level (ví dụ N5, N4...). Để trống = tất cả cấp độ.")
            String level
    ) {
        Long userId = currentUserService.getCurrentUserId();
        return flashcardSetService.getDashboard(userId, level);
    }

    // ===== NEW: Review 1 card trong set =====
    @PostMapping("/{setId}/cards/{cardId}/review")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Đánh dấu đã ôn 1 flashcard trong set",
            description = """
                    FE gọi API này mỗi khi user hoàn thành ôn 1 thẻ.
                    Hệ thống sẽ:
                    - Tăng review_count cho thẻ đó
                    - Cập nhật last_reviewed_at = now
                    - Nếu mastered=true trong body thì đánh dấu đã master
                    """
    )
    public ReviewCardResponse reviewCard(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @RequestBody(required = false) ReviewCardRequest req
    ) {
        User current = currentUserService.getCurrentUserOrThrow();

        // Use method with eager fetching to avoid LazyInitializationException
        FlashcardSet set = flashcardSetService.getSetOrThrowWithCreatedBy(setId);
        
        // Get card with eager fetch instead of accessing set.getCards() (which is lazy)
        Flashcard card = flashcardSetService.getCardByIdWithSet(cardId);
        
        // Verify card belongs to the set
        if (!card.getSet().getId().equals(setId)) {
            throw new EntityNotFoundException("Flashcard not in set");
        }

        // Authorization check
        if (set.getType() == FlashcardSetType.PERSONAL) {
            // PERSONAL: chỉ cho owner
            if (!set.getCreatedBy().getId().equals(current.getId())) {
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        } else if (set.getType() == FlashcardSetType.COURSE_VOCAB) {
            // COURSE_VOCAB: check enrollment or ownership
            // First, allow if user is the creator of the set (fallback for edge cases)
            if (set.getCreatedBy().getId().equals(current.getId())) {
                // User created this set, allow access
            } else if (set.getSectionContent() != null) {
                Long sectionContentId = set.getSectionContent().getId();
                boolean authorized = false;
                
                // Allow moderator if course is pending approval
                if (currentUserService.hasRole("MODERATOR")) {
                    try {
                        courseService.requireSectionContentBelongsToPendingApprovalCourse(sectionContentId);
                        // Validated, moderator can access
                        authorized = true;
                    } catch (ResponseStatusException e) {
                        // Not pending approval, continue with normal check
                    }
                }
                
                // If not authorized by moderator, check other permissions
                if (!authorized) {
                    // Check if user is course owner (teacher)
                    Long courseOwnerId = sectionsContentRepo.findCourseOwnerIdBySectionContentId(sectionContentId)
                            .orElse(null);
                    
                    if (courseOwnerId != null && courseOwnerId.equals(current.getId())) {
                        // Teacher owner, allow access
                        authorized = true;
                    } else {
                        // Not owner, check if learner is enrolled
                        Long courseId = sectionsContentRepo.findCourseIdBySectionContentId(sectionContentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found for this section content"));
                        
                        enrollmentRepo.findByUserIdAndCourseId(current.getId(), courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "You must enroll in this course to review flashcard cards"));
                        authorized = true;
                    }
                }
            } else {
                // SectionContent is null but set is COURSE_VOCAB - only allow creator
                throw new AccessDeniedException("You are not the owner of this flashcard set");
            }
        }

        boolean mastered = req != null && Boolean.TRUE.equals(req.mastered());

        UserFlashcardProgress progress =
                flashcardSetService.markCardReviewed(current, card, mastered);

        return new ReviewCardResponse(
                progress.getReviewCount(),
                progress.isMastered()
        );
    }
}
