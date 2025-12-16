package com.hokori.web.service;

import com.hokori.web.dto.certificate.CourseCompletionCertificateRes;
import com.hokori.web.entity.CourseCompletionCertificate;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseCompletionCertificateRepository;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseCompletionCertificateService {

    private final CourseCompletionCertificateRepository certRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;

    /**
     * Lấy tất cả certificates của một learner
     */
    public List<CourseCompletionCertificateRes> getMyCertificates(Long userId) {
        List<CourseCompletionCertificate> certificates = certRepo.findByUser_IdOrderByCompletedAtDesc(userId);
        
        return certificates.stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    /**
     * Lấy certificate của learner cho một course cụ thể
     */
    public CourseCompletionCertificateRes getCertificateByCourse(Long userId, Long courseId) {
        CourseCompletionCertificate cert = certRepo.findByUser_IdAndCourse_Id(userId, courseId)
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
        String courseTitle = null; // Luôn load từ course metadata để có tên mới nhất
        
        try {
            // Thử query với deleted_flag = false trước
            Optional<Object[]> courseMetadataOpt = courseRepo.findCourseMetadataById(cert.getCourseId());
            
            // Nếu không tìm thấy, thử query cả khi deleted_flag = true (để lấy title)
            if (courseMetadataOpt.isEmpty()) {
                log.debug("Course not found with deleted_flag=false for courseId={}, trying with deleted_flag=true", cert.getCourseId());
                courseMetadataOpt = courseRepo.findCourseMetadataByIdInternal(cert.getCourseId(), true);
            }
            
            if (courseMetadataOpt.isPresent()) {
                Object[] courseMetadata = courseMetadataOpt.get();
                log.debug("Found course metadata for courseId={}, array length={}", cert.getCourseId(), courseMetadata.length);
                
                // Handle nested array case (PostgreSQL)
                Object[] actualMetadata = courseMetadata;
                if (courseMetadata.length == 1 && courseMetadata[0] instanceof Object[]) {
                    actualMetadata = (Object[]) courseMetadata[0];
                    log.debug("Unwrapped nested array, new length={}", actualMetadata.length);
                }
                
                if (actualMetadata != null && actualMetadata.length > 1) {
                    // Ưu tiên lấy courseTitle từ course metadata (tên thực tế)
                    Object titleObj = actualMetadata[1];
                    if (titleObj != null) {
                        courseTitle = titleObj.toString().trim();
                        log.debug("Loaded courseTitle from metadata: '{}' for courseId={}", courseTitle, cert.getCourseId());
                    } else {
                        log.warn("courseMetadata[1] is null for courseId={}", cert.getCourseId());
                    }
                } else {
                    log.warn("actualMetadata is null or length < 2 for courseId={}, length={}", 
                            cert.getCourseId(), actualMetadata != null ? actualMetadata.length : 0);
                }
                
                if (actualMetadata != null && actualMetadata.length > 2) {
                    Object slugObj = actualMetadata[2];
                    if (slugObj != null) {
                        courseSlug = slugObj.toString();
                    }
                }
                
                if (actualMetadata != null && actualMetadata.length > 8) {
                    Object coverObj = actualMetadata[8];
                    if (coverObj != null) {
                        coverImagePath = coverObj.toString();
                    }
                }
            } else {
                // Course không tồn tại hoặc đã bị xóa (deleted_flag = true)
                log.warn("Course metadata not found for courseId={} (may be deleted or deleted_flag=true), certificateId={}", 
                        cert.getCourseId(), cert.getId());
            }
            
            // Fallback: nếu không load được từ metadata, dùng courseTitle trong certificate
            if (courseTitle == null || courseTitle.isEmpty()) {
                courseTitle = cert.getCourseTitle();
                if (courseTitle != null && !courseTitle.isEmpty()) {
                    log.debug("Using courseTitle from certificate snapshot for courseId={}, certificateId={}", 
                            cert.getCourseId(), cert.getId());
                }
            }
            
            // Fallback cuối cùng: nếu vẫn null, dùng courseId
            if (courseTitle == null || courseTitle.isEmpty()) {
                log.warn("Course title is null/empty for courseId={}, certificateId={}, using fallback", 
                        cert.getCourseId(), cert.getId());
                courseTitle = "Course #" + cert.getCourseId();
            }
        } catch (Exception e) {
            log.error("Failed to load course metadata for certificate {}, courseId={}", 
                    cert.getId(), cert.getCourseId(), e);
            // Fallback: dùng courseTitle trong certificate
            courseTitle = cert.getCourseTitle();
            if (courseTitle == null || courseTitle.isEmpty()) {
                courseTitle = "Course #" + cert.getCourseId();
            }
        }
        
        // Lấy thông tin learner name
        String learnerName = null;
        try {
            Optional<User> userOpt = userRepo.findById(cert.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                learnerName = (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty())
                        ? user.getDisplayName()
                        : (user.getUsername() != null && !user.getUsername().trim().isEmpty()
                            ? user.getUsername()
                            : "Learner");
            } else {
                learnerName = "Learner";
            }
        } catch (Exception e) {
            log.warn("Failed to load user info for certificate {}", cert.getId(), e);
            learnerName = "Learner";
        }
        
        return CourseCompletionCertificateRes.builder()
                .id(cert.getId())
                .enrollmentId(cert.getEnrollmentId())
                .courseId(cert.getCourseId())
                .courseTitle(courseTitle)
                .certificateNumber(cert.getCertificateNumber())
                .completedAt(cert.getCompletedAt())
                .issuedAt(cert.getIssuedAt())
                .courseSlug(courseSlug)
                .coverImagePath(coverImagePath)
                .learnerName(learnerName)
                .build();
    }
}

