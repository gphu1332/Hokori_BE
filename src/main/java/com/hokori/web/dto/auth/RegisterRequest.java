package com.hokori.web.dto.auth;

import com.hokori.web.constants.RoleConstants;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;

    private String displayName;
    private String country;
    private String nativeLanguage;
    private String currentJlptLevel; // "N5"... "N1"
    private String roleName; // LEARNER/TEACHER/STAFF/ADMIN

    public boolean isPasswordConfirmed(){
        return password != null && password.equals(confirmPassword);
    }
    public boolean isValidRole(){
        return roleName == null || switch (roleName.toUpperCase()) {
            case RoleConstants.LEARNER, RoleConstants.TEACHER, RoleConstants.STAFF, RoleConstants.ADMIN -> true;
            default -> false;
        };
    }
}
