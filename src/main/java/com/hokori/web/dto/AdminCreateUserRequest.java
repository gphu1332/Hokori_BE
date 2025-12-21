package com.hokori.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO cho admin tạo user mới với username/password và gán role
 */
@Data
public class AdminCreateUserRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String roleName;
    
    private String displayName;
    
    private Boolean isActive = true; // Default: active
    
    private Boolean isVerified = false; // Default: not verified
    
    private String currentJlptLevel; // Optional: N5, N4, N3, N2, N1
    
    private String firstName;
    
    private String lastName;
}

