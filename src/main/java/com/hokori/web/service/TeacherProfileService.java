package com.hokori.web.service;

import com.hokori.web.dto.TeacherApprovalDecisionRequest;
import com.hokori.web.dto.TeacherApprovalSubmitRequest;
import com.hokori.web.dto.TeacherProfileDTO;
import com.hokori.web.dto.TeacherQualificationUpdateRequest;
import com.hokori.web.dto.TeacherApprovalRequest;
import com.hokori.web.entity.TeacherProfile;
import com.hokori.web.entity.User;
import com.hokori.web.repository.RoleRepository;
import com.hokori.web.repository.TeacherApprovalRequestRepository;
import com.hokori.web.repository.TeacherProfileRepository;
import com.hokori.web.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// service/TeacherProfileService.java
@Service
@Transactional
public class TeacherProfileService {

    private final TeacherProfileRepository profileRepo;
    private final TeacherApprovalRequestRepository reqRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    public TeacherProfileService(TeacherProfileRepository profileRepo,
                                 TeacherApprovalRequestRepository reqRepo,
                                 UserRepository userRepo,
                                 RoleRepository roleRepo) {
        this.profileRepo = profileRepo;
        this.reqRepo = reqRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
    }

    /* Lấy/ cập nhật hồ sơ của chính giáo viên (userId lấy từ security hoặc truyền vào) */
    @Transactional(readOnly = true)
    public TeacherProfileDTO getMyProfile(Long userId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        TeacherProfile p = profileRepo.findByUser(u).orElseThrow(() -> new RuntimeException("Profile not found"));
        return TeacherProfileDTO.from(p);
    }

    public TeacherProfileDTO updateQualifications(Long userId, TeacherQualificationUpdateRequest req) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        TeacherProfile p = profileRepo.findByUser(u).orElseThrow(() -> new RuntimeException("Profile not found"));

        p.setHighestDegree(req.getHighestDegree());
        p.setMajor(req.getMajor());
        p.setYearsOfExperience(req.getYearsOfExperience());
        p.setCertifications(req.getCertifications());
        p.setEvidenceUrls(req.getEvidenceUrls());

        // Cập nhật lại trạng thái nếu đang REJECTED -> quay lại DRAFT khi sửa
        if (p.getApprovalStatus() == TeacherProfile.ApprovalStatus.REJECTED) {
            p.setApprovalStatus(TeacherProfile.ApprovalStatus.DRAFT);
            p.setRejectedAt(null);
        }
        return TeacherProfileDTO.from(profileRepo.save(p));
    }

    /* Giáo viên gửi yêu cầu duyệt */
    public Long submitApproval(Long userId, TeacherApprovalSubmitRequest req) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        TeacherProfile p = profileRepo.findByUser(u).orElseThrow(() -> new RuntimeException("Profile not found"));

        // Đảm bảo user có role TEACHER
        if (u.getRole() == null || !"TEACHER".equals(u.getRole().getRoleName()))
            throw new RuntimeException("Only TEACHER can request approval");

        // Chỉ cho submit khi đang DRAFT hoặc REJECTED
        if (p.getApprovalStatus() == TeacherProfile.ApprovalStatus.PENDING)
            throw new RuntimeException("Request already submitted");
        if (p.getApprovalStatus() == TeacherProfile.ApprovalStatus.APPROVED)
            throw new RuntimeException("Already approved");

        p.setApprovalStatus(TeacherProfile.ApprovalStatus.PENDING);
        profileRepo.save(p);

        TeacherApprovalRequest r = new TeacherApprovalRequest();
        r.setProfile(p);
        r.setMessageFromTeacher(req.getMessage());
        r = reqRepo.save(r);

        return r.getId();
    }

    /* Admin xem danh sách pending */
    @Transactional(readOnly = true)
    public List<TeacherApprovalRequest> getPendingRequests() {
        return reqRepo.findByStatusOrderBySubmittedAtAsc(TeacherApprovalRequest.Status.PENDING);
    }

    /* Admin quyết định duyệt/từ chối */
    public void decide(Long requestId, Long adminId, TeacherApprovalDecisionRequest req) {
        TeacherApprovalRequest r = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        User admin = userRepo.findById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));

        TeacherProfile p = r.getProfile();
        r.setReviewedAt(LocalDateTime.now());
        r.setReviewer(admin);
        r.setReviewNote(req.getNote());

        if (Boolean.TRUE.equals(req.getApprove())) {
            r.setStatus(TeacherApprovalRequest.Status.APPROVED);
            p.setApprovalStatus(TeacherProfile.ApprovalStatus.APPROVED);
            p.setApprovedAt(LocalDateTime.now());
            p.setRejectedAt(null);
        } else {
            r.setStatus(TeacherApprovalRequest.Status.REJECTED);
            p.setApprovalStatus(TeacherProfile.ApprovalStatus.REJECTED);
            p.setRejectedAt(LocalDateTime.now());
        }
        profileRepo.save(p);
        reqRepo.save(r);
    }

    /* Quyền tạo course: chỉ cho phép khi profile APPROVED */
    @Transactional(readOnly = true)
    public boolean canCreateCourse(Long userId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        TeacherProfile p = profileRepo.findByUser(u).orElseThrow(() -> new RuntimeException("Profile not found"));
        return p.getApprovalStatus() == TeacherProfile.ApprovalStatus.APPROVED;
    }
}

