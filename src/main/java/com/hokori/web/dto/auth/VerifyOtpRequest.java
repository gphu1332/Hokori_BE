package com.hokori.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO để verify OTP
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Email or phone number is required")
    private String emailOrPhone;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otpCode;
}

