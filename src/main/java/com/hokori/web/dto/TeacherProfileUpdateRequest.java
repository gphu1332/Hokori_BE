package com.hokori.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherProfileUpdateRequest {
    // Info nghề nghiệp
    private Integer yearsOfExperience;
    private String bio;
    private String teachingStyles;

    // Social
    @Size(max = 255) private String websiteUrl;
    @Size(max = 255) private String linkedin;

    // Payout/Bank
    @Size(max = 100) private String bankAccountNumber;
    @Size(max = 150) private String bankAccountName;
    @Size(max = 150) private String bankName;
    @Size(max = 150) private String bankBranchName;
}
