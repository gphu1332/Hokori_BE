package com.hokori.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterTeacherRequest {
    @NotBlank private String username;
    @NotBlank @Email private String email;
    @NotBlank private String password;

    // hồ sơ (đã gộp vào User)
    private String firstName;
    private String lastName;
    private String headline;
    private String bio; // ≥ 50 ký tự

    private String currentJlptLevel; // yêu cầu N2/N1

    // socials (optional)
    private String websiteUrl;
    private String linkedin;
}
