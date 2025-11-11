package com.hokori.web.repository;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.entity.ProfileApproveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileApproveRequestRepository extends JpaRepository<ProfileApproveRequest, Long> {
    List<ProfileApproveRequest> findByStatus(ApprovalStatus status);
    List<ProfileApproveRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<ProfileApproveRequest> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
    boolean existsByUser_IdAndStatus(Long userId, ApprovalStatus status);
}
