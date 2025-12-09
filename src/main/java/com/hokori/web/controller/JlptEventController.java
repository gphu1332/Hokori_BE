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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

        // Validate dates
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // startAt không được là quá khứ
        if (req.getStartAt().isBefore(now)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "startAt không được là quá khứ. Thời gian bắt đầu phải sau thời điểm hiện tại."
            );
        }
        
        // endAt phải sau startAt
        if (req.getEndAt().isBefore(req.getStartAt()) || req.getEndAt().isEqual(req.getStartAt())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "endAt phải sau startAt. Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

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

    // ===== Admin/Moderator/Teacher: xem tất cả event =====
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR','TEACHER')")
    @Operation(
            summary = "List tất cả JLPT event (Admin/Moderator/Teacher)",
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
                    
                    ⚠️ QUAN TRỌNG: Endpoint này CHỈ trả về events có status = "OPEN".
                    KHÔNG BAO GIỜ trả về "DRAFT" hoặc "CLOSED".
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Danh sách events đang OPEN (CHỈ trả về status = 'OPEN', không bao giờ trả về 'DRAFT' hoặc 'CLOSED')",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = JlptEventResponse.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Open Events Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "JLPT N5 Mock Test - Tháng 12/2024",
                                                "level": "N5",
                                                "description": "Đợt thi thử JLPT N5",
                                                "status": "OPEN",
                                                "startAt": "2024-12-01T00:00:00",
                                                "endAt": "2024-12-31T23:59:59",
                                                "createdByUserId": 1
                                              }
                                            ]
                                            """
                            )
                    }
            )
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
