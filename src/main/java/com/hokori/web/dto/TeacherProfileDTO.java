package com.hokori.web.dto;

import com.hokori.web.entity.TeacherProfile;

// dto/TeacherProfileDto.java  (để trả ra FE)
public record TeacherProfileDTO(
        Long id, Long userId, String displayName,
        String firstName, String lastName, String headline, String bio,
        String websiteUrl, String facebook, String instagram, String linkedin,
        String tiktok, String x, String youtube, String language,
        String highestDegree, String major, Integer yearsOfExperience,
        String certifications, String evidenceUrls,
        String approvalStatus
) {
    public static TeacherProfileDTO from(TeacherProfile p) {
        return new TeacherProfileDTO(
                p.getId(), p.getUser().getId(), p.getUser().getDisplayName(),
                p.getFirstName(), p.getLastName(), p.getHeadline(), p.getBio(),
                p.getWebsiteUrl(), p.getFacebook(), p.getInstagram(), p.getLinkedin(),
                p.getTiktok(), p.getX(), p.getYoutube(), p.getLanguage(),
                p.getHighestDegree(), p.getMajor(), p.getYearsOfExperience(),
                p.getCertifications(), p.getEvidenceUrls(),
                p.getApprovalStatus().name()
        );
    }
}

