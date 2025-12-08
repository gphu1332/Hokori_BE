package com.hokori.web.service;

import com.hokori.web.dto.certificate.CourseCompletionCertificateRes;
import com.hokori.web.entity.CourseCompletionCertificate;
import com.hokori.web.repository.CourseCompletionCertificateRepository;
import com.hokori.web.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseCompletionCertificateService {

    private final CourseCompletionCertificateRepository certRepo;
    private final CourseRepository courseRepo;

    /**
     * Lấy tất cả certificates của một learner
     */
    public List<CourseCompletionCertificateRes> getMyCertificates(Long userId) {
        List<CourseCompletionCertificate> certificates = certRepo.findByUserIdOrderByCompletedAtDesc(userId);
        
        return certificates.stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    /**
     * Lấy certificate của learner cho một course cụ thể
     */
    public CourseCompletionCertificateRes getCertificateByCourse(Long userId, Long courseId) {
        CourseCompletionCertificate cert = certRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Certificate not found for this course"));
        
        return toRes(cert);
    }

    /**
     * Lấy certificate theo ID (chỉ owner mới xem được)
     */
    public CourseCompletionCertificateRes getCertificateById(Long certificateId, Long userId) {
        CourseCompletionCertificate cert = certRepo.findById(certificateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        
        if (!cert.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to view this certificate");
        }
        
        return toRes(cert);
    }

    /**
     * Map entity to DTO
     */
    private CourseCompletionCertificateRes toRes(CourseCompletionCertificate cert) {
        // Lấy thêm thông tin course nếu cần
        String courseSlug = null;
        String coverImagePath = null;
        
        try {
            Object[] courseMetadata = courseRepo.findCourseMetadataById(cert.getCourseId()).orElse(null);
            if (courseMetadata != null && courseMetadata.length > 2) {
                courseSlug = courseMetadata[2] != null ? courseMetadata[2].toString() : null;
            }
            if (courseMetadata != null && courseMetadata.length > 8) {
                coverImagePath = courseMetadata[8] != null ? courseMetadata[8].toString() : null;
            }
        } catch (Exception e) {
            log.warn("Failed to load course metadata for certificate {}", cert.getId(), e);
        }
        
        return CourseCompletionCertificateRes.builder()
                .id(cert.getId())
                .enrollmentId(cert.getEnrollmentId())
                .courseId(cert.getCourseId())
                .courseTitle(cert.getCourseTitle())
                .certificateNumber(cert.getCertificateNumber())
                .completedAt(cert.getCompletedAt())
                .issuedAt(cert.getIssuedAt())
                .courseSlug(courseSlug)
                .coverImagePath(coverImagePath)
                .build();
    }
}

