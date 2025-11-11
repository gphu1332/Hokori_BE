package com.hokori.web.dto;

import java.time.LocalDate;

public record UserCertificateReq(
        String title,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialId,
        String credentialUrl,
        String fileUrl,
        String fileName,
        String mimeType,
        Long   fileSizeBytes,
        String storageProvider,
        String note
) {}

