package com.hokori.web.repository;

import com.hokori.web.dto.TeacherApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// repository/TeacherApprovalRequestRepository.java
public interface TeacherApprovalRequestRepository extends JpaRepository<TeacherApprovalRequest, Long> {
    List<TeacherApprovalRequest> findByProfileIdOrderBySubmittedAtDesc(Long profileId);
    List<TeacherApprovalRequest> findByStatusOrderBySubmittedAtAsc(TeacherApprovalRequest.Status status);
}
