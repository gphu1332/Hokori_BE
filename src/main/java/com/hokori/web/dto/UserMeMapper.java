package com.hokori.web.dto;

import com.hokori.web.entity.User;

// mapper/UserMeMapper.java
public final class UserMeMapper {
    private UserMeMapper(){}

    public static UserMeResponse toResponse(User u) {
        UserMeResponse r = new UserMeResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setUsername(u.getUsername());
        r.setDisplayName(u.getDisplayName());
        r.setAvatarUrl(u.getAvatarUrl());
        r.setPhoneNumber(u.getPhoneNumber());
        r.setCountry(u.getCountry());
        r.setIsActive(u.getIsActive());
        r.setIsVerified(u.getIsVerified());
        r.setLastLoginAt(u.getLastLoginAt());
        r.setCreatedAt(u.getCreatedAt());
        r.setRole(u.getRole()!=null ? u.getRole().getRoleName() : null);

        boolean isTeacher = u.getRole()!=null && "TEACHER".equals(u.getRole().getRoleName());
        boolean hasTeacherFlow = u.getApprovalStatus()!=null && u.getApprovalStatus()!=com.hokori.web.Enum.ApprovalStatus.NONE;

        if (isTeacher || hasTeacherFlow) {
            UserMeResponse.TeacherSection t = new UserMeResponse.TeacherSection();
            t.setApprovalStatus(u.getApprovalStatus());
            t.setApprovedAt(u.getApprovedAt());
            t.setYearsOfExperience(u.getYearsOfExperience());
            t.setBio(u.getBio());
            t.setTeachingStyles(u.getTeachingStyles());
            t.setWebsiteUrl(u.getWebsiteUrl());
            t.setFacebook(u.getFacebook());
            t.setInstagram(u.getInstagram());
            t.setLinkedin(u.getLinkedin());
            t.setTiktok(u.getTiktok());
            t.setX(u.getX());
            t.setYoutube(u.getYoutube());
            t.setBankAccountNumber(u.getBankAccountNumber());
            t.setBankAccountName(u.getBankAccountName());
            t.setBankName(u.getBankName());
            t.setBankBranchName(u.getBankBranchName());
            t.setLastPayoutDate(u.getLastPayoutDate());
            t.setCurrentApproveRequestId(
                    u.getCurrentApproveRequest()!=null ? u.getCurrentApproveRequest().getId() : null
            );
            r.setTeacher(t);
        }
        return r;
    }
}

