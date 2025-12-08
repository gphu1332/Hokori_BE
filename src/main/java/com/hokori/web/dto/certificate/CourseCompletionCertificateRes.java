package com.hokori.web.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO cho course completion certificate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCompletionCertificateRes {
    private Long id;
    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String certificateNumber;
    private Instant completedAt;
    private Instant issuedAt;
    
    // Thông tin thêm từ course (optional, có thể load sau)
    private String courseSlug;
    private String coverImagePath;
}

