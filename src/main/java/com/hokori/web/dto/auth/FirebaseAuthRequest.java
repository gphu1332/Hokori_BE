package com.hokori.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirebaseAuthRequest {
    @NotBlank
    private String firebaseToken;
    
    /**
     * Role to assign when registering new user via Google OAuth.
     * Only LEARNER or TEACHER are allowed for Google registration.
     * Defaults to LEARNER if not provided.
     */
    private String role;
}
