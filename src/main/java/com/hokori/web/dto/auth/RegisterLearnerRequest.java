package com.hokori.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterLearnerRequest {
    @NotBlank private String username;
    @NotBlank @Email private String email;
    @NotBlank private String password;

    private String displayName;
    private String country;
    private String nativeLanguage;
    private String currentJlptLevel; // optional
}
