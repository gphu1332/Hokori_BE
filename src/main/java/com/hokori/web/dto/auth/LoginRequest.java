package com.hokori.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String username;  // có thể là email
    @NotBlank private String password;
}
