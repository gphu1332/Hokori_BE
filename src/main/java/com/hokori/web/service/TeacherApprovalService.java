package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.dto.*;
import com.hokori.web.entity.*;
import com.hokori.web.mapper.ApprovalMapper;
import com.hokori.web.repository.ProfileApproveRequestItemRepository;
import com.hokori.web.repository.ProfileApproveRequestRepository;
import com.hokori.web.repository.UserCertificateRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TeacherApprovalService {

    private final UserRepository userRepo;
    private final UserCertificateRepository certRepo;
    private final ProfileApproveRequestRepository reqRepo;
    private final ProfileApproveRequestItemRepository itemRepo;
    private final ApprovalMapper mapper;

    /* ===== Certificates ===== */

    @Transactional(readOnly = true)
    public List<UserCertificateDto> listMyCertificates(Long userId){
        return certRepo.findByUser_Id(userId).stream().map(mapper::toDto).toList();
    }

    public UserCertificateDto addCertificate(Long userId, UserCertificateReq req){
        User u = userRepo.findById(userId).orElseThrow();
        UserCertificate c = new UserCertificate();
        c.setUser(u);
        c.setTitle(req.title());
        c.setIssueDate(req.issueDate());
        c.setExpiryDate(req.expiryDate());
        c.setCredentialId(req.credentialId());
        c.setCredentialUrl(req.credentialUrl());
        c.setFileUrl(req.fileUrl());
        c.setFileName(req.fileName());
        c.setMimeType(req.mimeType());
        c.setFileSizeBytes(req.fileSizeBytes());
        c.setStorageProvider(req.storageProvider());
        c.setNote(req.note());
        return mapper.toDto(certRepo.save(c));
    }

    public UserCertificateDto updateCertificate(Long userId, Long certId, UserCertificateReq req){
        UserCertificate c = certRepo.findById(certId).orElseThrow();
        if (!c.getUser().getId().equals(userId)) throw new IllegalArgumentException("Not your certificate");
        if (req.title()!=null) c.setTitle(req.title());
        if (req.issueDate()!=null) c.setIssueDate(req.issueDate());
        if (req.expiryDate()!=null) c.setExpiryDate(req.expiryDate());
        if (req.credentialId()!=null) c.setCredentialId(req.credentialId());
        if (req.credentialUrl()!=null) c.setCredentialUrl(req.credentialUrl());
        if (req.fileUrl()!=null) c.setFileUrl(req.fileUrl());
        if (req.fileName()!=null) c.setFileName(req.fileName());
        if (req.mimeType()!=null) c.setMimeType(req.mimeType());
        if (req.fileSizeBytes()!=null) c.setFileSizeBytes(req.fileSizeBytes());
        if (req.storageProvider()!=null) c.setStorageProvider(req.storageProvider());
        if (req.note()!=null) c.setNote(req.note());
        return mapper.toDto(certRepo.save(c));
    }

    public void deleteCertificate(Long userId, Long certId){
        UserCertificate c = certRepo.findById(certId).orElseThrow();
        if (!c.getUser().getId().equals(userId)) throw new IllegalArgumentException("Not your certificate");
        certRepo.delete(c);
    }

    /* ===== Submit Approval ===== */

    public ApproveRequestDto submitApproval(Long userId, SubmitApprovalReq req){
        User u = userRepo.findById(userId).orElseThrow();

        // Không cho nộp khi đang PENDING
        if (reqRepo.existsByUser_IdAndStatus(userId, ApprovalStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending request");
        }

        // Lấy danh sách certificate để snapshot
        List<UserCertificate> selected;
        if (req != null && req.certificateIds()!=null && !req.certificateIds().isEmpty()){
            selected = certRepo.findAllById(req.certificateIds());
            // lọc chỉ của user
            selected = selected.stream().filter(c -> c.getUser().getId().equals(userId)).toList();
        } else {
            selected = certRepo.findByUser_Id(userId);
        }

        if (selected.isEmpty()){
            throw new IllegalArgumentException("No certificates to submit");
        }

        ProfileApproveRequest r = new ProfileApproveRequest();
        r.setUser(u);
        r.setStatus(ApprovalStatus.PENDING);
        r.setSubmittedAt(LocalDateTime.now());
        r.setNote(req != null ? req.note() : null);

        List<ProfileApproveRequestItem> items = new ArrayList<>();
        for (UserCertificate c : selected){
            ProfileApproveRequestItem i = new ProfileApproveRequestItem();
            i.setRequest(r);
            i.setSourceCertificate(c); // link gốc
            // snapshot
            i.setTitle(c.getTitle());
            i.setIssueDate(c.getIssueDate());
            i.setExpiryDate(c.getExpiryDate());
            i.setCredentialId(c.getCredentialId());
            i.setCredentialUrl(c.getCredentialUrl());
            i.setFileUrl(c.getFileUrl());
            i.setFileName(c.getFileName());
            i.setMimeType(c.getMimeType());
            i.setFileSizeBytes(c.getFileSizeBytes());
            i.setStorageProvider(c.getStorageProvider());
            items.add(i);
        }
        r.setItems(items);

        // cập nhật trạng thái user
        u.setApprovalStatus(ApprovalStatus.PENDING);
        u.setApprovedAt(null);
        u.setApprovedByUserId(null);

        // lưu
        reqRepo.save(r);
        u.setCurrentApproveRequest(r);
        userRepo.save(u);

        return mapper.toDto(r);
    }

    @Transactional(readOnly = true)
    public ApproveRequestDto getLatestRequest(Long userId){
        ProfileApproveRequest r = reqRepo.findFirstByUser_IdOrderByCreatedAtDesc(userId).orElseThrow();
        r.getItems().size(); // force init
        return mapper.toDto(r);
    }

    /* ===== Admin quyết định ===== */

    public ApproveRequestDto adminDecide(Long requestId, Long adminUserId, ApproveDecisionReq req){
        if (req.action() == null) throw new IllegalArgumentException("Missing action");
        if (req.action()!=ApprovalStatus.APPROVED && req.action()!=ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Action must be APPROVED or REJECTED");
        }
        ProfileApproveRequest r = reqRepo.findById(requestId).orElseThrow();
        if (r.getStatus()!=ApprovalStatus.PENDING) {
            throw new IllegalStateException("Request already decided");
        }

        r.setStatus(req.action());
        r.setReviewedBy(adminUserId);
        r.setReviewedAt(LocalDateTime.now());
        r.setNote(req.note());
        reqRepo.save(r);

        // cập nhật User
        User u = r.getUser();
        u.setApprovalStatus(req.action());
        u.setApprovedAt(LocalDateTime.now());
        u.setApprovedByUserId(adminUserId);
        if (req.action()==ApprovalStatus.APPROVED) {
            u.setCurrentApproveRequest(r); // giữ tham chiếu
        }
        userRepo.save(u);

        r.getItems().size();
        return mapper.toDto(r);
    }
}
