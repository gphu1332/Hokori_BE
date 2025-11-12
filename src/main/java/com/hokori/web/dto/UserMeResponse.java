package com.hokori.web.dto;

import lombok.Data;

@Data
public class UserMeResponse {
    // Base (dùng chung)
    private Long id;
    private String email;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String phoneNumber;
    private String country;
    private Boolean isActive;
    private Boolean isVerified;
    private java.time.LocalDateTime lastLoginAt;
    private java.time.LocalDateTime createdAt;

    private String role; // ví dụ: "LEARNER"/"TEACHER"/"ADMIN"/"MODERATOR"

    // Chỉ có nếu user là TEACHER (hoặc đang xin duyệt)
    private TeacherSection teacher; // nullable

    @lombok.Data
    public static class TeacherSection {
        private com.hokori.web.Enum.ApprovalStatus approvalStatus;
        private java.time.LocalDateTime approvedAt;
        private Integer yearsOfExperience;
        private String bio;
        private String teachingStyles;
        private String websiteUrl, facebook, instagram, linkedin, tiktok, x, youtube;

        // Payout
        private String bankAccountNumber;
        private String bankAccountName;
        private String bankName;
        private String bankBranchName;
        private java.time.LocalDate lastPayoutDate;

        // Hiển thị tiến độ duyệt (nếu cần)
        private Long currentApproveRequestId;
    }
}

