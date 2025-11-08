package com.hokori.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApproveRequestItemDto(
        Long id,
        Long sourceCertificateId,
        String title,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialId,
        String credentialUrl,
        String fileUrl,
        String fileName,
        String mimeType,
        Long fileSizeBytes,
        String storageProvider,
        Long verifiedBy,
        LocalDateTime verifiedAt,
        String note
) {}

