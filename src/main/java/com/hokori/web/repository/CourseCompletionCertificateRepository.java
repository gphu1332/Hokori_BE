package com.hokori.web.repository;

import com.hokori.web.entity.CourseCompletionCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCompletionCertificateRepository extends JpaRepository<CourseCompletionCertificate, Long> {

    /**
     * Tìm certificate theo enrollment ID
     */
    Optional<CourseCompletionCertificate> findByEnrollmentId(Long enrollmentId);

    /**
     * Lấy tất cả certificates của một user
     */
    List<CourseCompletionCertificate> findByUserIdOrderByCompletedAtDesc(Long userId);

    /**
     * Kiểm tra user đã có certificate cho course này chưa
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Lấy certificate của user cho một course cụ thể
     */
    Optional<CourseCompletionCertificate> findByUserIdAndCourseId(Long userId, Long courseId);
}

