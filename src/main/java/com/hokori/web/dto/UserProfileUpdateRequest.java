package com.hokori.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
    
    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;
}