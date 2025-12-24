package com.hokori.web.mapper;

import com.hokori.web.dto.ApproveRequestDto;
import com.hokori.web.dto.ApproveRequestItemDto;
import com.hokori.web.dto.UserCertificateDto;
import com.hokori.web.entity.ProfileApproveRequest;
import com.hokori.web.entity.ProfileApproveRequestItem;
import com.hokori.web.entity.UserCertificate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApprovalMapper {

    public UserCertificateDto toDto(UserCertificate c){
        return new UserCertificateDto(
                c.getId(), c.getTitle(),
                c.getIssueDate(), c.getExpiryDate(),
                c.getCredentialId(), c.getCredentialUrl(),
                c.getFileUrl(), c.getFileName(), c.getMimeType(), c.getFileSizeBytes(), c.getStorageProvider(),
                c.getVerifiedBy(), c.getVerifiedAt(),
                c.getNote()
        );
    }

    public ApproveRequestItemDto toDto(ProfileApproveRequestItem i){
        return new ApproveRequestItemDto(
                i.getId(),
                i.getSourceCertificate() != null ? i.getSourceCertificate().getId() : null,
                i.getTitle(), i.getIssueDate(), i.getExpiryDate(),
                i.getCredentialId(), i.getCredentialUrl(),
                i.getFileUrl(), i.getFileName(), i.getMimeType(), i.getFileSizeBytes(), i.getStorageProvider(),
                i.getVerifiedBy(), i.getVerifiedAt(),
                i.getNote()
        );
    }

    public ApproveRequestDto toDto(ProfileApproveRequest r){
        List<ApproveRequestItemDto> items = r.getItems().stream().map(this::toDto).toList();
        
        // Get teacher info from User
        String teacherName = null;
        String email = null;
        String phoneNumber = null;
        if (r.getUser() != null) {
            String displayName = r.getUser().getDisplayName();
            String username = r.getUser().getUsername();
            teacherName = (displayName != null && !displayName.trim().isEmpty()) 
                    ? displayName 
                    : username;
            email = r.getUser().getEmail();
            phoneNumber = r.getUser().getPhoneNumber();
        }
        
        return new ApproveRequestDto(
                r.getId(),
                r.getUser().getId(),
                teacherName,
                email,
                phoneNumber,
                r.getStatus(),
                r.getSubmittedAt(),
                r.getReviewedBy(),
                r.getReviewedAt(),
                r.getNote(),
                items
        );
    }
}
