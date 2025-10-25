package com.hokori.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterLearnerRequest {
    @NotBlank @Size(min = 3, max = 100)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 255)
    private String displayName;

    @Size(max = 100)
    private String country;

    @Size(max = 50)
    private String nativeLanguage;

    /** JLPT: N5, N4, N3, N2, N1 (optional) */
    private String currentJlptLevel;
}