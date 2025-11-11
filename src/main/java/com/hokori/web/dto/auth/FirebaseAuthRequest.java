package com.hokori.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirebaseAuthRequest {
    @NotBlank
    private String firebaseToken;
}
