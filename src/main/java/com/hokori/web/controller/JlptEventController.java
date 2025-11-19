// com.hokori.web.controller.JlptEventController.java
package com.hokori.web.controller;

import com.hokori.web.Enum.JlptEventStatus;
import com.hokori.web.dto.jlpt.JlptEventCreateRequest;
import com.hokori.web.dto.jlpt.JlptEventResponse;
import com.hokori.web.dto.jlpt.JlptEventStatusUpdateRequest;
import com.hokori.web.entity.JlptEvent;
import com.hokori.web.entity.User;
import com.hokori.web.repository.JlptEventRepository;
import com.hokori.web.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/jlpt/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "JLPT Events", description = "Quản lý sự kiện thi JLPT")
public class JlptEventController {

    private final JlptEventRepository eventRepo;
    private final CurrentUserService currentUserService;

    // ===== Admin: tạo event =====
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin tạo JLPT Event",
            description = """
                    Tạo 1 đợt thi JLPT:
                    - level: N5, N4, ...
                    - startAt, endAt: thời gian diễn ra
                    - status: nếu null thì mặc định DRAFT
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tạo thành công",
            content = @Content(schema = @Schema(implementation = JlptEventResponse.class))
    )
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public JlptEventResponse createEvent(
            @Valid @RequestBody JlptEventCreateRequest req
    ) {
        User admin = currentUserService.getCurrentUserOrThrow();

        JlptEvent e = JlptEvent.builder()
                .createdBy(admin)
                .title(req.getTitle())
                .level(req.getLevel())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : JlptEventStatus.DRAFT)
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .deletedFlag(false)
                .build();

        eventRepo.save(e);
        return JlptEventResponse.fromEntity(e);
    }

    // ===== Admin/Moderator: xem tất cả event =====
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(
            summary = "List tất cả JLPT event (Admin/Moderator)",
            description = "Có thể filter theo status, level"
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = JlptEventResponse.class)))
    )
    public List<JlptEventResponse> listEvents(
            @Parameter(description = "Status lọc (optional)")
            @RequestParam(required = false) JlptEventStatus status,
            @Parameter(description = "Level JLPT, ví dụ N5, N4 (optional)")
            @RequestParam(required = false) String level
    ) {
        List<JlptEvent> events;
        if (status != null && level != null) {
            events = eventRepo.findByStatusAndLevelAndDeletedFlagFalse(status, level);
        } else if (status != null) {
            events = eventRepo.findByStatusAndDeletedFlagFalse(status);
        } else if (level != null) {
            events = eventRepo.findByLevelAndDeletedFlagFalse(level);
        } else {
            events = eventRepo.findByDeletedFlagFalse();
        }

        return events.stream()
                .map(JlptEventResponse::fromEntity)
                .toList();
    }

    // ===== Learner: xem các event đang OPEN =====

    @GetMapping("/open")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Learner: list JLPT event đang OPEN",
            description = """
                    FE dùng để hiển thị danh sách đợt thi đang mở cho user.
                    Có thể filter level, ví dụ:
                    GET /api/jlpt/events/open?level=N3
                    """
    )
    public List<JlptEventResponse> listOpenEvents(
            @RequestParam(required = false) String level
    ) {
        List<JlptEvent> events;
        if (level != null) {
            events = eventRepo.findByStatusAndLevelAndDeletedFlagFalse(JlptEventStatus.OPEN, level);
        } else {
            events = eventRepo.findByStatusAndDeletedFlagFalse(JlptEventStatus.OPEN);
        }
        return events.stream()
                .map(JlptEventResponse::fromEntity)
                .toList();
    }

    // ===== Admin: đổi status event =====

    @PatchMapping("/{eventId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin đổi status của JLPT Event",
            description = "Ví dụ: DRAFT -> OPEN, OPEN -> CLOSED, ..."
    )
    public JlptEventResponse updateStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody JlptEventStatusUpdateRequest req
    ) {
        JlptEvent e = eventRepo.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        e.setStatus(req.getStatus());
        eventRepo.save(e);

        return JlptEventResponse.fromEntity(e);
    }
}
