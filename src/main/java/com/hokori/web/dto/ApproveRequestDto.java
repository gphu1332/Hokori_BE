package com.hokori.web.dto;

import com.hokori.web.Enum.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApproveRequestDto(
        Long id,
        Long userId,
        String teacherName, // Tên teacher (displayName hoặc username)
        String email, // Email của teacher
        String phoneNumber, // Số điện thoại của teacher
        ApprovalStatus status,
        LocalDateTime submittedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String note,
        List<ApproveRequestItemDto> items
) {}
