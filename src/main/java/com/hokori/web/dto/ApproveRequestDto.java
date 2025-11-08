package com.hokori.web.dto;

import com.hokori.web.Enum.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApproveRequestDto(
        Long id,
        Long userId,
        ApprovalStatus status,
        LocalDateTime submittedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String note,
        List<ApproveRequestItemDto> items
) {}
